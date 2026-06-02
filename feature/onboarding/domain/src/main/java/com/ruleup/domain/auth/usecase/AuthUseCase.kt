package com.ruleup.domain.auth.usecase

import com.ruleup.domain.auth.model.AuthSession
import com.ruleup.domain.auth.model.OAuthProvider
import com.ruleup.domain.auth.model.OAuthResult
import com.ruleup.domain.auth.model.SignupForm
import com.ruleup.domain.auth.repository.AuthRepository
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import com.ruleup.domain.auth.repository.SessionRepository
import com.ruleup.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class AuthUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val oAuthProvider: OAuthAuthorizer,
        private val sessionRepository: SessionRepository,
        private val profileRepository: ProfileRepository,
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
            val imageUrl =
                form.localImageUri
                    ?.takeIf { it.isNotBlank() }
                    ?.let { profileRepository.uploadProfileImage(it) }
            val session =
                authRepository.signup(
                    signupToken = form.signupToken,
                    nickname = form.nickname,
                    interestCategories = form.interestCategories,
                    profileImageUrl = imageUrl,
                    agreements = form.agreements,
                )

            sessionRepository.saveSession(session)

            return session
        }

        suspend fun checkNicknameAvailability(nickname: String): Boolean = authRepository.checkNicknameAvailability(nickname).available
    }
