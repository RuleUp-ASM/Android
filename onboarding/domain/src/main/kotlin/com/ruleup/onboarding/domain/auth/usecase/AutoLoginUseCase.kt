package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.domain.token.TokenRepository
import com.ruleup.logging.domain.BizLogger
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.logging.OnboardingEvents
import com.ruleup.onboarding.domain.logging.SessionExpiredTrigger
import java.io.IOException
import javax.inject.Inject

/** 자동 로그인의 결과. */
sealed interface AutoLoginResult {
    /** 세션 복구 성공. */
    data object Authenticated : AutoLoginResult

    /** 저장된 세션이 없다. */
    data object NoSession : AutoLoginResult

    /** 세션이 실제로 끝났다(401). */
    data object SessionExpired : AutoLoginResult

    /** 전송 실패. */
    data object ConnectionFailed : AutoLoginResult
}

/** 자동 로그인. */
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
                        // 동시 토큰 갱신 실패 시 최신 저장 토큰 확인.
                        val latest = tokenRepository.getRefreshToken()
                        if (latest != null && latest != refreshToken) {
                            AutoLoginResult.Authenticated
                        } else if (it is IOException) {
                            // 전송 실패는 세션이 끊긴 게 아니다.
                            AutoLoginResult.ConnectionFailed
                        } else {
                            // 세션이 실제로 끊긴 지점.
                            bizLogger.record(OnboardingEvents.sessionExpired(SessionExpiredTrigger.EXPIRED))
                            tokenRepository.clear()
                            AutoLoginResult.SessionExpired
                        }
                    },
                )
        }
    }
