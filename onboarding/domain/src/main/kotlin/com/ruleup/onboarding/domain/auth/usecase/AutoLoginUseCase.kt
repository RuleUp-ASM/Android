package com.ruleup.onboarding.domain.auth.usecase
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.observability.OnboardingEvents
import com.ruleup.onboarding.domain.observability.SessionExpiredTrigger
import javax.inject.Inject

/** 자동 로그인. 저장된 refreshToken 으로 앱 토큰을 재발급(명세 4.4)해 세션을 복구한다. */
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
                        // 갱신 응답이 userId 를 함께 줘서 프로필 조회 없이 세션이 완성된다.
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
                            // 세션이 실제로 끊긴 지점. 다른 기기 로그인 때문인지 단순 만료인지는
                            // 서버가 둘 다 401 SESSION_EXPIRED 로 내려 구분할 수 없다.
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
