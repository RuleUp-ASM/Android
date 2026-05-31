package com.ruleup.data.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SignupRequest(
    @SerialName("signup_token")
    val signupToken: String,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("interest_categories")
    val interestCategories: List<String>,
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
    @SerialName("agreements")
    val agreements: AgreementsDto,
)

@Serializable
data class AgreementsDto(
    @SerialName("terms")
    val terms: Boolean,
    @SerialName("privacy")
    val privacy: Boolean,
    @SerialName("marketing")
    val marketing: Boolean = false,
)
