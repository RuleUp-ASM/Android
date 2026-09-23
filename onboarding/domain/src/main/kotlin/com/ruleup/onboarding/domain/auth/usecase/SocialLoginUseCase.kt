package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.onboarding.domain.auth.entity.LoginOutcome
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import com.ruleup.onboarding.domain.auth.entity.PermissionSnapshot
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import javax.inject.Inject

private const val TAG = "[Login]"

/**
 * 소셜 로그인 응답을 화면이 갈 곳([LoginOutcome])으로 정규화한다. 기존 회원이면 **분기와 무관하게
 * 먼저 세션을 저장한다** — 잠금·닉네임 충돌 화면도 인증된 API 를 호출해야 한다.
 */
class SocialLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val deviceIdentityRepository: DeviceIdentityRepository,
        private val tokenRepository: TokenRepository,
        private val observability: Observability,
    ) {
        suspend operator fun invoke(
            authorization: OAuthAuthorization,
            permissions: PermissionSnapshot? = null,
        ): LoginOutcome {
            val device = deviceIdentityRepository.current()
            return when (val result = authRepository.exchangeToken(authorization, device, permissions)) {
                is OAuthResult.ExistingUser -> {
                    val user = result.session.user
                    tokenRepository.saveSession(result.session.token, user.id)
                    // 명세는 "false 면 온보딩 화면으로" 라지만 그 화면이 요구하는 signupToken 이 기존
                    // 회원 응답에는 없다. 보내 봐야 제출에서 막히므로 실제로 오는지 기록만 남긴다.
                    if (!user.onboardingCompleted) {
                        observability.w(TAG) { "기존 회원인데 onboardingCompleted=false — signupToken 이 없어 온보딩을 이어갈 수 없다" }
                    }
                    when {
                        user.nicknameStatus == NicknameStatus.CONFLICT ->
                            LoginOutcome.ResetNickname(user.nickname)

                        // 서버는 정지를 `SUSPENDED` 로 내린다. `LOCKED` 만 보면 **재로그인이 게이트를
                        // 통째로 지나쳐** 잠긴 계정이 홈에 들어간다(AUTH-13). 종류는 화면이 가른다.
                        user.accountStatus != AccountStatus.ACTIVE ->
                            LoginOutcome.Restricted(user.lockInfo)

                        else -> LoginOutcome.GoHome(restored = result.restored)
                    }
                }

                is OAuthResult.NewUser ->
                    LoginOutcome.GoSignup(
                        signupToken = result.signupToken,
                        expiresInSeconds = result.expiresInSeconds,
                        profile = result.profile,
                    )
            }
        }
    }
