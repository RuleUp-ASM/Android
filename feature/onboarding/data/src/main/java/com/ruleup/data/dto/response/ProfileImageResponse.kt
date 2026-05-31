package com.ruleup.data.dto.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileImageResponse(
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
)
