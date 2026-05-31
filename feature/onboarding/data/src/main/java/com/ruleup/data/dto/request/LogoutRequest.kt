package com.ruleup.data.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LogoutRequest(
    @SerialName("refresh_token")
    val refreshToken: String,
)
