package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import com.ruleup.onboarding.domain.entity.AccountStatus
import com.ruleup.onboarding.domain.entity.LoginOutcome
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.entity.PermissionSnapshot
import com.ruleup.profile.domain.entity.NicknameStatus
import javax.inject.Inject

/**
 * 소셜 로그인. 응답을 화면이 갈 곳([LoginOutcome])으로 정규화한다.
 *
 * 기존 회원이면 **분기와 무관하게 먼저 세션을 저장한다.** 잠금·닉네임 충돌도 로그인 자체는 성공한
 * 상태이고, 이후 화면(열람 전용 홈·닉네임 재설정)이 인증된 API 를 호출해야 하기 때문이다.
 *
 * 영구 정지·동일 설치 다계정은 403 이라 여기까지 오지 않고 예외로 전파된다.
 */
class SocialLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val deviceIdentityRepository: DeviceIdentityRepository,
        private val tokenRepository: TokenRepository,
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
                    when {
                        // 복원 과정에서 닉네임을 선점당했다. 바꾸기 전엔 홈으로 보내지 않는다.
                        user.nicknameStatus == NicknameStatus.CONFLICT ->
                            LoginOutcome.ResetNickname(user.nickname)

                        user.accountStatus == AccountStatus.LOCKED ->
                            LoginOutcome.GoHomeReadOnly(user.lockInfo)

                        else -> LoginOutcome.GoHome
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
