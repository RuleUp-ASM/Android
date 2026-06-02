package com.ruleup.data.auth.dto.response

import com.ruleup.core.model.Tokens
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
)

internal fun TokenResponse.toDomain(): Tokens =
    Tokens(
        accessToken = accessToken.requireField("access_token"),
        refreshToken = refreshToken.requireField("refresh_token"),
        tokenType = tokenType ?: "Bearer",
        expiresInSeconds = expiresIn.requireField("expires_in"),
    )
