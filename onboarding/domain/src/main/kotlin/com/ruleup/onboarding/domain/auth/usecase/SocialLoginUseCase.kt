package com.ruleup.onboarding.domain.auth.usecase
import com.ruleup.analytics.AnalyticsEvent
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.domain.token.TokenRepository
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.entity.LoginResult
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import javax.inject.Inject

class SocialLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
        private val analyticsLogger: AnalyticsLogger,
    ) {
        suspend operator fun invoke(authorization: OAuthAuthorization): LoginResult =
            when (val result = authRepository.exchangeToken(authorization)) {
                is OAuthResult.ExistingUser -> {
                    tokenRepository.saveTokens(result.session.token)
                    // 기존 회원의 로그인 완료 시점. 신규 회원(NewUser)은 가입 완료 전이라 로그인으로 보지 않는다.
                    analyticsLogger.log(AnalyticsEvent.Login(provider = authorization.provider.provider))
                    LoginResult.GoMain
                }

                is OAuthResult.NewUser -> {
                    LoginResult.GoSignup(result.signupToken)
                }
            }
    }
