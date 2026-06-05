package com.ruleup.domain.onboarding

import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.onboarding.LoginResult
import com.ruleup.entity.onboarding.OAuthAuthorization
import com.ruleup.entity.onboarding.OAuthResult
import javax.inject.Inject

class SocialLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val tokenRepository: TokenRepository,
    ) {
        suspend operator fun invoke(authorization: OAuthAuthorization): LoginResult =
            when (val result = authRepository.exchangeToken(authorization)) {
                is OAuthResult.ExistingUser -> {
                    tokenRepository.saveTokens(result.session.token)
                    LoginResult.GoMain
                }

                is OAuthResult.NewUser -> {
                    LoginResult.GoSignup(result.signupToken)
                }
            }
    }
