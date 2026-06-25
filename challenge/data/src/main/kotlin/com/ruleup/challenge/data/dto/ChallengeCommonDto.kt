package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.Penalty
import com.ruleup.challenge.domain.entity.Reward
import com.ruleup.challenge.domain.entity.SnsShare
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** SNS 공유 패널티 설정. 요청/응답 공용. */
@Serializable
data class SnsShareDto(
    @SerialName("enabled")
    val enabled: Boolean? = null,
    @SerialName("phone")
    val phone: String? = null,
)

internal fun SnsShareDto.toDomain(): SnsShare =
    SnsShare(
        enabled = enabled ?: false,
        phone = phone,
    )

internal fun SnsShare.toDto(): SnsShareDto =
    SnsShareDto(
        enabled = enabled,
        phone = phone,
    )

/** 패널티 설정. 요청/응답 공용. */
@Serializable
data class PenaltyDto(
    @SerialName("mannerDeduction")
    val mannerDeduction: Double? = null,
    @SerialName("snsShare")
    val snsShare: SnsShareDto? = null,
    @SerialName("groupShare")
    val groupShare: Boolean? = null,
)

internal fun PenaltyDto.toDomain(): Penalty =
    Penalty(
        mannerDeduction = mannerDeduction ?: 0.0,
        snsShare = snsShare?.toDomain() ?: SnsShare(enabled = false, phone = null),
        groupShare = groupShare ?: false,
    )

internal fun Penalty.toDto(): PenaltyDto =
    PenaltyDto(
        mannerDeduction = mannerDeduction,
        snsShare = snsShare.toDto(),
        groupShare = groupShare,
    )

/** 보상 설정. 요청/응답 공용. */
@Serializable
data class RewardDto(
    @SerialName("mannerGain")
    val mannerGain: Double? = null,
)

internal fun RewardDto.toDomain(): Reward =
    Reward(
        mannerGain = mannerGain ?: 0.0,
    )

internal fun Reward.toDto(): RewardDto =
    RewardDto(
        mannerGain = mannerGain,
    )
