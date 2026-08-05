package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.token.RefreshedSession
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.LogoutRequest
import com.ruleup.onboarding.data.auth.dto.SignUpRequest
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.toAuthSession
import com.ruleup.onboarding.data.auth.dto.toOAuthResult
import com.ruleup.onboarding.data.auth.dto.toRefreshedSession
import com.ruleup.onboarding.data.auth.dto.toRequest
import com.ruleup.onboarding.data.auth.mapAuthFailure
import com.ruleup.onboarding.data.device.DeviceInfoProvider
import com.ruleup.onboarding.domain.auth.model.SignupForm
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.entity.AuthSession
import com.ruleup.onboarding.domain.entity.DeviceIdentity
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import com.ruleup.onboarding.domain.entity.PermissionSnapshot
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
        private val deviceInfoProvider: DeviceInfoProvider,
    ) : AuthRepository {
        override suspend fun exchangeToken(
            authorization: OAuthAuthorization,
            device: DeviceIdentity,
            permissions: PermissionSnapshot?,
        ): OAuthResult =
            mapAuthFailure {
                api
                    .socialLogin(
                        provider = authorization.provider.provider,
                        request =
                            SocialLoginAuthRequest(
                                code = authorization.code,
                                codeVerifier = authorization.codeVerifier,
                                redirectUri = authorization.redirectUri,
                                deviceId = device.deviceId,
                                installationId = device.installationId,
                                deviceInfo = deviceInfoProvider.current(),
                                permissions = permissions?.toRequest(),
                            ),
                    ).getOrThrow()
                    .toOAuthResult()
            }

        override suspend fun signup(
            form: SignupForm,
            device: DeviceIdentity,
        ): AuthSession =
            mapAuthFailure {
                api
                    .signup(
                        request =
                            SignUpRequest(
                                signupToken = form.signupToken,
                                nickname = form.nickname,
                                interestCategories = form.interestCategories.map { it.value },
                                birthDate = form.birthDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                gender = form.gender.value,
                                agreements = form.agreements.toRequest(),
                                deviceId = device.deviceId,
                                installationId = device.installationId,
                                deviceInfo = deviceInfoProvider.current(),
                            ),
                    ).getOrThrow()
                    .toAuthSession()
            }

        override suspend fun refreshToken(refreshToken: String): RefreshedSession =
            api
                .refreshToken(TokenRefreshRequest(refreshToken = refreshToken))
                .getOrThrow()
                .toRefreshedSession()

        override suspend fun logout(refreshToken: String) {
            api.logout(LogoutRequest(refreshToken = refreshToken)).throwOnError()
        }
    }
