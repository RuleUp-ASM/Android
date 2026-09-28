package com.ruleup.onboarding.data.auth.repository

import com.ruleup.domain.device.DeviceIdentity
import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.domain.token.TokenRefresher
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.LogoutRequest
import com.ruleup.onboarding.data.auth.dto.SignUpRequest
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.auth.dto.WithdrawRequest
import com.ruleup.onboarding.data.auth.dto.toAuthFailure
import com.ruleup.onboarding.data.auth.dto.toAuthSession
import com.ruleup.onboarding.data.auth.dto.toDomain
import com.ruleup.onboarding.data.auth.dto.toOAuthResult
import com.ruleup.onboarding.data.auth.dto.toRequest
import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import com.ruleup.onboarding.domain.auth.entity.AuthSession
import com.ruleup.onboarding.domain.auth.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.auth.entity.OAuthResult
import com.ruleup.onboarding.domain.auth.entity.PermissionSnapshot
import com.ruleup.onboarding.domain.auth.entity.SignupForm
import com.ruleup.onboarding.domain.auth.entity.Withdrawal
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import java.io.IOException
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
        private val deviceInfoProvider: DeviceInfoProvider,
        private val tokenRefresher: TokenRefresher,
        private val signupInviteStore: com.ruleup.onboarding.domain.auth.repository.SignupInviteStore,
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
                                deviceInfo = deviceInfoProvider.current().toRequest(),
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
                                inviteLink = signupInviteStore.currentLink(),
                                nickname = form.nickname,
                                interestCategories = form.interestCategories.map { it.value },
                                birthDate = form.birthDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                gender = form.gender.value,
                                agreements = form.agreements.toRequest(),
                                deviceId = device.deviceId,
                                installationId = device.installationId,
                                deviceInfo = deviceInfoProvider.current().toRequest(),
                            ),
                    ).getOrThrow()
                    .toAuthSession()
                    .also { signupInviteStore.clear() }
            }

        // 콜드스타트 자동로그인도 Authenticator 와 같은 갱신기를 거쳐야 같은 토큰으로 두 번 보내지 않는다.
        override suspend fun refreshToken(refreshToken: String): RefreshedSession =
            tokenRefresher.refresh(refreshToken)
                ?: throw ApiException(code = "SESSION_EXPIRED", message = "세션이 만료되었습니다.")

        override suspend fun logout(refreshToken: String) {
            api.logout(LogoutRequest(refreshToken = refreshToken)).throwOnError()
        }

        override suspend fun withdraw(confirmPhrase: String): Withdrawal =
            api
                .withdraw(WithdrawRequest(confirmPhrase = confirmPhrase))
                .getOrThrow()
                .toDomain()
    }

/** 인증 계열 호출의 실패를 [AuthException] 으로 통일한다. */
private suspend fun <T> mapAuthFailure(block: suspend () -> T): T =
    try {
        block()
    } catch (e: ApiException) {
        throw AuthException(e.toAuthFailure(), e.message, e)
    } catch (e: IOException) {
        throw AuthException(AuthFailure.NETWORK, e.message, e)
    }
