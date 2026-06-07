package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import javax.inject.Inject

/**
 * 로그아웃 유스케이스.
 *
 * 1) 서버에 현재 기기 refreshToken revoke 를 요청한다(명세 4.5). 실패해도 로컬 로그아웃은 진행한다.
 * 2) 로컬에 저장된 토큰을 모두 삭제한다.
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
