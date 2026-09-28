package com.ruleup.verification.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubmitAppealRequest(
    // 필수, 10자 이상
    @SerialName("reason")
    val reason: String,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)
