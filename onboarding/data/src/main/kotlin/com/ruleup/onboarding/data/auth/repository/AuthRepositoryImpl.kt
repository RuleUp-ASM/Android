package com.ruleup.onboarding.data.auth.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.ruleup.domain.entity.user.Agreement
import com.ruleup.domain.entity.user.AuthSession
import com.ruleup.domain.entity.user.InterestCategory
import com.ruleup.domain.entity.user.Token
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.onboarding.data.auth.api.AuthApi
import com.ruleup.onboarding.data.auth.dto.AgreementsRequest
import com.ruleup.onboarding.data.auth.dto.ClientPropertiesRequest
import com.ruleup.onboarding.data.auth.dto.DeviceInfoRequest
import com.ruleup.onboarding.data.auth.dto.LogoutRequest
import com.ruleup.onboarding.data.auth.dto.SignUpRequest
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.toAuthSession
import com.ruleup.onboarding.data.auth.dto.toOAuthResult
import com.ruleup.onboarding.data.auth.dto.toToken
import com.ruleup.onboarding.domain.auth.repository.AuthRepository
import com.ruleup.onboarding.domain.entity.OAuthAuthorization
import com.ruleup.onboarding.domain.entity.OAuthResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AuthRepositoryImpl
    @Inject
    constructor(
        private val api: AuthApi,
        @ApplicationContext private val context: Context,
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
                            deviceInfo = context.deviceInfo(),
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
                            deviceInfo = context.deviceInfo(),
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

private fun Agreement.toClientProperties(): ClientPropertiesRequest =
    ClientPropertiesRequest(
        agreements =
            AgreementsRequest(
                terms = terms,
                privacy = privacy,
                marketing = marketing,
            ),
    )

/** 공통 기기 정보 채집(명세 4.1~4.3). Build/ActivityManager/PackageManager 에서 읽는다. */
private fun Context.deviceInfo(): DeviceInfoRequest {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionCode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    return DeviceInfoRequest(
        platform = "ANDROID",
        osVersion = Build.VERSION.RELEASE.orEmpty(),
        sdkInt = Build.VERSION.SDK_INT,
        deviceModel = Build.MODEL.orEmpty(),
        manufacturer = Build.MANUFACTURER.orEmpty(),
        lowRam = getSystemService(ActivityManager::class.java)?.isLowRamDevice ?: false,
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = versionCode,
    )
}
