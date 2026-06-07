package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import javax.inject.Inject

/**
 * 자동 로그인 유스케이스.
 *
 * 저장된 refreshToken 으로 앱 토큰을 재발급(명세 4.4)해 세션을 복구한다.
 * - refreshToken 이 없으면 → false (로그인 필요).
 * - 재발급 성공 → 새 토큰을 저장하고 true (홈 진입).
 * - 재발급 실패(만료·서버 오류 등) → 로컬 토큰을 정리하고 false (로그인 필요).
 */
class AutoLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(): Boolean {
            val refreshToken = tokenRepository.getRefreshToken() ?: return false
            return runCatching { authRepository.refreshToken(refreshToken) }
                .onSuccess { tokenRepository.saveTokens(it) }
                .onFailure { tokenRepository.clear() }
                .isSuccess
        }
    }
