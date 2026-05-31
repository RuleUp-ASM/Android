package com.ruleup.data.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("interest_categories")
    val interestCategories: List<String>? = null,
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
)
