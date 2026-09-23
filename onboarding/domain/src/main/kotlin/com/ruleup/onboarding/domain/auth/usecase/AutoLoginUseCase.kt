package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.logging.domain.BizLogger
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.logging.OnboardingEvents
import com.ruleup.onboarding.domain.logging.SessionExpiredTrigger
import java.io.IOException
import javax.inject.Inject

/**
 * 자동 로그인의 결과.
 *
 * **불리언으로 접으면 안 된다.** 세션이 끝난 것과 연결이 안 된 것은 사용자가 할 일이 다른데,
 * 둘 다 "false" 로 돌려주면 진입 화면이 구분할 수 없어 **타임아웃 한 번에 로그인 화면으로 떨어진다**
 * (ENV-03). 로그인한 적 없는 상태까지 셋이 갈린다.
 */
sealed interface AutoLoginResult {
    /** 세션 복구 성공. */
    data object Authenticated : AutoLoginResult

    /** 저장된 세션이 없다. 처음 켰거나 로그아웃한 상태다. */
    data object NoSession : AutoLoginResult

    /** 세션이 실제로 끝났다(401). 로컬 토큰도 정리됐으므로 로그인부터 다시 한다. */
    data object SessionExpired : AutoLoginResult

    /** 전송 실패. **세션은 살아 있다** — 토큰을 남겨 두고 다시 시도하게 한다. */
    data object ConnectionFailed : AutoLoginResult
}

/** 자동 로그인. 저장된 refreshToken 으로 앱 토큰을 재발급(명세 4.4)해 세션을 복구한다. */
class AutoLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
        private val bizLogger: BizLogger,
    ) {
        suspend operator fun invoke(): AutoLoginResult {
            val refreshToken = tokenRepository.getRefreshToken() ?: return AutoLoginResult.NoSession
            return runCatching { authRepository.refreshToken(refreshToken) }
                .fold(
                    onSuccess = {
                        // 갱신 응답이 userId 를 함께 줘서 프로필 조회 없이 세션이 완성된다.
                        tokenRepository.saveTokens(it.token, it.userId)
                        AutoLoginResult.Authenticated
                    },
                    onFailure = {
                        // 콜드스타트 동시 갱신 레이스: 인터셉터(TokenAuthenticator)가 이미 refreshToken 을
                        // 회전시켰다면 이쪽 요청은 낡은 토큰이라 실패한다. 이때 세션은 유효하므로 정리하지 않는다.
                        val latest = tokenRepository.getRefreshToken()
                        if (latest != null && latest != refreshToken) {
                            AutoLoginResult.Authenticated
                        } else if (it is IOException) {
                            // 전송 실패는 세션이 끊긴 게 아니다. 여기서 지우면 타임아웃 한 번에 아직 며칠
                            // 남은 refreshToken 이 버려지고 소셜 로그인부터 다시 해야 한다. 토큰을 남겨
                            // 다시 시도하게 한다.
                            AutoLoginResult.ConnectionFailed
                        } else {
                            // 세션이 실제로 끊긴 지점. 다른 기기 로그인 때문인지 단순 만료인지는
                            // 서버가 둘 다 401 SESSION_EXPIRED 로 내려 구분할 수 없다.
                            bizLogger.record(OnboardingEvents.sessionExpired(SessionExpiredTrigger.EXPIRED))
                            tokenRepository.clear()
                            AutoLoginResult.SessionExpired
                        }
                    },
                )
        }
    }
