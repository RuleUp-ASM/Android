package com.ruleup.data.repository

import com.ruleup.data.api.AuthApi
import com.ruleup.data.dto.AgreementsDto
import com.ruleup.data.dto.ClientPropertiesDto
import com.ruleup.data.dto.LogoutRequest
import com.ruleup.data.dto.SignUpRequest
import com.ruleup.data.dto.SocialLoginAuthRequest
import com.ruleup.data.dto.TokenRefreshRequest
import com.ruleup.data.dto.toAuthSession
import com.ruleup.data.dto.toOAuthResult
import com.ruleup.data.dto.toToken
import com.ruleup.domain.onboarding.AuthRepository
import com.ruleup.entity.onboarding.OAuthAuthorization
import com.ruleup.entity.onboarding.OAuthResult
import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.AuthSession
import com.ruleup.entity.user.InterestCategory
import com.ruleup.entity.user.Token
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
    ) : AuthRepository {
        override suspend fun exchangeToken(authorization: OAuthAuthorization): OAuthResult =
            api
                .socialLogin(
                    provider = authorization.provider.provider,
                    request =
                        SocialLoginAuthRequest(
                            code = authorization.code,
                            codeVerifier = authorization.codeVerifier,
                            redirectUri = authorization.redirectUri,
                        ),
                ).getOrThrow()
                .toOAuthResult()

        override suspend fun signup(
            signupToken: String,
            nickname: String,
            interestCategories: List<InterestCategory>,
            profileImageUrl: String?,
            agreements: Agreement,
        ): AuthSession =
            api
                .signup(
                    request =
                        SignUpRequest(
                            signupToken = signupToken,
                            nickname = nickname,
                            interestCategories = interestCategories.map { it.value },
                            profileImageUrl = profileImageUrl,
                            clientProperties = agreements.toClientProperties(),
                        ),
                ).getOrThrow()
                .toAuthSession()

        override suspend fun refreshToken(refreshToken: String): Token =
            api
                .refreshToken(TokenRefreshRequest(refreshToken = refreshToken))
                .getOrThrow()
                .toToken()

        override suspend fun logout(refreshToken: String) {
            api.logout(LogoutRequest(refreshToken = refreshToken)).throwOnError()
        }
    }

private fun Agreement.toClientProperties(): ClientPropertiesDto =
    ClientPropertiesDto(
        agreements =
            AgreementsDto(
                terms = terms,
                privacy = privacy,
                marketing = marketing,
            ),
    )
