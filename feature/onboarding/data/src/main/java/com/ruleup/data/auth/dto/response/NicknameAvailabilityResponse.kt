package com.ruleup.data.auth.dto.response

import com.ruleup.domain.auth.model.NicknameAvailability
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NicknameAvailabilityResponse(
    @SerialName("available")
    val available: Boolean? = null,
    @SerialName("reason")
    val reason: String? = null,
)

internal fun NicknameAvailabilityResponse.toDomain(): NicknameAvailability =
    NicknameAvailability(
        available = available.requireField("available"),
        reason = reason,
    )
