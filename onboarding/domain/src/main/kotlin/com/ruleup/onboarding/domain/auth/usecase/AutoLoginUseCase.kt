package com.ruleup.onboarding.domain.auth.usecase
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
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
                .fold(
                    onSuccess = { tokenRepository.saveTokens(it); true },
                    onFailure = {
                        // 콜드스타트 동시 갱신 레이스: 인터셉터(TokenAuthenticator)가 이미 refreshToken 을
                        // 회전시켰다면 이쪽 요청은 낡은 토큰이라 실패한다. 이때 세션은 유효하므로 정리하지 않는다.
                        val latest = tokenRepository.getRefreshToken()
                        if (latest != null && latest != refreshToken) {
                            true
                        } else {
                            tokenRepository.clear()
                            false
                        }
                    },
                )
        }
    }
