package com.ruleup.domain.auth.usecase

import com.ruleup.domain.auth.repository.OAuthAuthorizer
import com.ruleup.domain.model.AuthSession
import com.ruleup.domain.model.OAuthProvider
import com.ruleup.domain.model.OAuthResult
import com.ruleup.domain.model.SignupForm
import com.ruleup.domain.repository.AuthRepository
import com.ruleup.domain.repository.SessionRepository
import javax.inject.Inject

class AuthUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val oAuthProvider: OAuthAuthorizer,
        private val sessionRepository: SessionRepository,
    ) {
        suspend fun login(provider: OAuthProvider): OAuthResult {
            val auth = oAuthProvider.authorize(provider)
            val result =
                authRepository.oauthLogin(
                    provider = provider,
                    code = auth.code,
                    codeVerifier = auth.codeVerifier,
                    redirectUri = auth.redirectUri,
                )

            if (result is OAuthResult.ExistingUser) {
                sessionRepository.saveSession(result.session)
            }

            return result
        }

        suspend fun signUp(form: SignupForm): AuthSession {
            val session =
                authRepository.signup(
                    signupToken = form.signupToken,
                    nickname = form.nickname,
                    interestCategories = form.interestCategories,
                    profileImageUrl = form.profileImageUrl,
                    agreements = form.agreements,
                )

            sessionRepository.saveSession(session)

            return session
        }
    }
