package com.ruleup.onboarding.domain.auth.usecase
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.observability.OnboardingEvents
import com.ruleup.onboarding.domain.observability.SessionExpiredTrigger
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
        private val observability: Observability,
    ) {
        suspend operator fun invoke(): Boolean {
            val refreshToken = tokenRepository.getRefreshToken() ?: return false
            return runCatching { authRepository.refreshToken(refreshToken) }
                .fold(
                    onSuccess = {
                        // 갱신 응답이 userId 를 함께 주므로 여기서 세션이 완성된다 — 예전엔 이 값이
                        // 비어 프로필 조회로 따로 메워야 했다.
                        tokenRepository.saveTokens(it.token, it.userId)
                        true
                    },
                    onFailure = {
                        // 콜드스타트 동시 갱신 레이스: 인터셉터(TokenAuthenticator)가 이미 refreshToken 을
                        // 회전시켰다면 이쪽 요청은 낡은 토큰이라 실패한다. 이때 세션은 유효하므로 정리하지 않는다.
                        val latest = tokenRepository.getRefreshToken()
                        if (latest != null && latest != refreshToken) {
                            true
                        } else {
                            // 여기가 세션이 실제로 끊긴 지점이다 — 자진 로그아웃은 refreshToken 이 이미
                            // 없어 위에서 일찍 빠지므로 여기 오지 않는다.
                            //
                            // 다른 기기 로그인 때문인지 단순 만료인지는 **계약상 구분할 수 없다**.
                            // 서버가 둘 다 401 SESSION_EXPIRED 로 내려서, 트리거 분리는 응답에
                            // 사유가 실린 뒤에나 가능하다.
                            observability.log(Channel.BUSINESS) {
                                OnboardingEvents.sessionExpired(SessionExpiredTrigger.EXPIRED)
                            }
                            tokenRepository.clear()
                            false
                        }
                    },
                )
        }
    }
