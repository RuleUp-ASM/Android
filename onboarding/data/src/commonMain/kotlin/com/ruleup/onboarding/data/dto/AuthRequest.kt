package com.ruleup.onboarding.data.dto

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
