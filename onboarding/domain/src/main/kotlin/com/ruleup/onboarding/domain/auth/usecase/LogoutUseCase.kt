package com.ruleup.onboarding.domain.auth.usecase
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import javax.inject.Inject

/**
 * 로그아웃. 서버에 현재 기기 refreshToken revoke 를 요청(명세 4.5)한 뒤 로컬 토큰을 지운다 —
 * revoke 가 실패해도 로컬 로그아웃은 진행한다.
 */
class LogoutUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke() {
            tokenRepository.getRefreshToken()?.let { refreshToken ->
                runCatching { authRepository.logout(refreshToken) }
            }
            tokenRepository.clear()
        }
    }
