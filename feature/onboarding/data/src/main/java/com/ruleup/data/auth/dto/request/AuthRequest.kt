package com.ruleup.data.auth.dto.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    @SerialName("code")
    val code: String,
    @SerialName("code_verifier")
    val codeVerifier: String,
    @SerialName("redirect_uri")
    val redirectUri: String,
)
