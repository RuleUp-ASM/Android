package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.device.DeviceIdentityRepository
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
import javax.inject.Inject

private const val TAG = "[Login]"

/** 소셜 로그인 응답을 화면이 갈 곳([LoginOutcome])으로 정규화한다. */
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
                    val account = requireNotNull(user.account)
                    tokenRepository.saveSession(result.session.token, user.id)
                    // 명세는 "false 면 온보딩 화면으로" 라지만 그 화면이 요구하는 signupToken 이 기존 회원 응답에는 없다.
                    if (!account.onboardingCompleted) {
                        observability.w(TAG) { "기존 회원인데 onboardingCompleted=false — signupToken 이 없어 온보딩을 이어갈 수 없다" }
                    }
                    when {
                        account.nicknameStatus == NicknameStatus.CONFLICT ->
                            LoginOutcome.ResetNickname(user.nickname)

                        // 서버는 정지를 `SUSPENDED` 로 내린다.
                        account.accountStatus != AccountStatus.ACTIVE ->
                            LoginOutcome.Restricted(account.lockInfo)

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
