package com.ruleup.data.repository

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.model.Tokens
import com.ruleup.data.api.AuthApi
import com.ruleup.data.dto.request.AgreementsDto
import com.ruleup.data.dto.request.AuthRequest
import com.ruleup.data.dto.request.LogoutRequest
import com.ruleup.data.dto.request.RefreshRequest
import com.ruleup.data.dto.request.SignupRequest
import com.ruleup.data.dto.response.toAuthSession
import com.ruleup.data.dto.response.toDomain
import com.ruleup.data.dto.response.toOAuthResult
import com.ruleup.domain.model.Agreements
import com.ruleup.domain.model.AuthSession
import com.ruleup.domain.model.NicknameAvailability
import com.ruleup.domain.model.OAuthProvider
import com.ruleup.domain.model.OAuthResult
import com.ruleup.domain.repository.AuthRepository
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val authApi: AuthApi,
    ) : AuthRepository {
        override suspend fun oauthLogin(
            provider: OAuthProvider,
            code: String,
            codeVerifier: String,
            redirectUri: String,
        ): OAuthResult =
            authApi
                .oauthLogin(
                    provider = provider.value,
                    request =
                        AuthRequest(
                            code = code,
                            codeVerifier = codeVerifier,
                            redirectUri = redirectUri,
                        ),
                ).getOrThrow()
                .toOAuthResult()

        override suspend fun signup(
            signupToken: String,
            nickname: String,
            interestCategories: List<InterestCategory>,
            profileImageUrl: String?,
            agreements: Agreements,
        ): AuthSession =
            authApi
                .signup(
                    request =
                        SignupRequest(
                            signupToken = signupToken,
                            nickname = nickname,
                            interestCategories = interestCategories.map { it.value },
                            profileImageUrl = profileImageUrl,
                            agreements =
                                AgreementsDto(
                                    terms = agreements.terms,
                                    privacy = agreements.privacy,
                                    marketing = agreements.marketing,
                                ),
                        ),
                ).getOrThrow()
                .toAuthSession()

        override suspend fun refresh(refreshToken: String): Tokens = authApi.refresh(RefreshRequest(refreshToken)).getOrThrow().toDomain()

        override suspend fun logout(refreshToken: String) {
            authApi.logout(LogoutRequest(refreshToken)).throwOnError()
        }

        override suspend fun checkNicknameAvailability(nickname: String): NicknameAvailability =
            authApi.checkNickname(nickname).getOrThrow().toDomain()
    }
