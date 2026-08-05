package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.token.RefreshedSession
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.LogoutRequest
import com.ruleup.onboarding.data.auth.dto.SignUpRequest
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.toAuthFailure
import com.ruleup.onboarding.data.auth.dto.toAuthSession
import com.ruleup.onboarding.data.auth.dto.toOAuthResult
import com.ruleup.onboarding.data.auth.dto.toRefreshedSession
import com.ruleup.onboarding.data.auth.dto.toRequest
import com.ruleup.onboarding.data.device.DeviceInfoProvider
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.DeviceIdentity
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import com.ruleup.onboarding.domain.auth.entity.PermissionSnapshot
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import java.io.IOException
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

/**
 * 인증 계열 호출을 감싸 실패를 [AuthException] 으로 통일한다.
 *
 * 네트워크 오류([IOException])는 재시도로 풀릴 수 있어 따로 구분한다 — 서버가 준 에러와 섞으면
 * 화면이 "다시 시도"를 권할지 "로그인부터 다시"를 권할지 정할 수 없다.
 */
private suspend fun <T> mapAuthFailure(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ApiException) {
        throw AuthException(e.toAuthFailure(), e.message, e)
    } catch (e: IOException) {
        throw AuthException(AuthFailure.NETWORK, e.message, e)
    }
