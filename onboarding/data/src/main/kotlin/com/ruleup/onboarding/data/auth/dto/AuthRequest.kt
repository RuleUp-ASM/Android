package com.ruleup.onboarding.data.auth.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginAuthRequest(
    @SerialName("code")
    val code: String? = null,
    @SerialName("codeVerifier")
    val codeVerifier: String? = null,
    @SerialName("redirectUri")
    val redirectUri: String? = null,
    // 기기·SDK·버전 정보. 매 로그인마다 동반해 서버가 최신 1건으로 갱신한다(명세 4.1/4.2).
    @SerialName("deviceInfo")
    val deviceInfo: DeviceInfoDto? = null,
)

/** 공통 기기 정보 객체(명세 4.1~4.3). 로그인·가입 양쪽에 동반해 기기·SDK·앱버전을 저장한다. */
@Serializable
data class DeviceInfoDto(
    @SerialName("platform")
    val platform: String = "ANDROID",
    @SerialName("osVersion")
    val osVersion: String? = null,
    @SerialName("sdkInt")
    val sdkInt: Int? = null,
    @SerialName("deviceModel")
    val deviceModel: String? = null,
    @SerialName("manufacturer")
    val manufacturer: String? = null,
    @SerialName("lowRam")
    val lowRam: Boolean? = null,
    @SerialName("versionName")
    val versionName: String? = null,
    @SerialName("versionCode")
    val versionCode: Int? = null,
)

@Serializable
data class ClientPropertiesDto(
    @SerialName("agreements")
    val agreements: AgreementsDto? = null,
)

@Serializable
data class AgreementsDto(
    @SerialName("terms")
    val terms: Boolean? = null,
    @SerialName("privacy")
    val privacy: Boolean? = null,
    @SerialName("marketing")
    val marketing: Boolean? = null,
)

@Serializable
data class SignUpRequest(
    @SerialName("signupToken")
    val signupToken: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("interestCategories")
    val interestCategories: List<String>? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    // 약관 동의를 가입 완료 단계에서 함께 전달한다(명세 4.3).
    @SerialName("clientProperties")
    val clientProperties: ClientPropertiesDto? = null,
    // 신규 회원의 기기 정보 확정 저장(명세 4.3, 로그인·가입 양쪽 동반).
    @SerialName("deviceInfo")
    val deviceInfo: DeviceInfoDto? = null,
)

@Serializable
data class TokenRefreshRequest(
    @SerialName("refreshToken")
    val refreshToken: String? = null,
)

@Serializable
data class LogoutRequest(
    @SerialName("refreshToken")
    val refreshToken: String? = null,
)
