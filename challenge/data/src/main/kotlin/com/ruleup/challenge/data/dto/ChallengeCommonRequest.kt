package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ParamEntry
import com.ruleup.challenge.domain.entity.VerificationConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 기간 요청. */
@Serializable
data class PeriodRequest(
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
)

internal fun ChallengePeriod.toRequest(): PeriodRequest = PeriodRequest(start = start, end = end)

/** 인증 요청. */
@Serializable
data class VerificationRequest(
    @SerialName("type") val type: String,
    @SerialName("method") val method: String,
)

internal fun VerificationConfig.toRequest(): VerificationRequest = VerificationRequest(type = type.value, method = method.value)

/** 패널티 요청. */
@Serializable
data class PenaltiesRequest(
    @SerialName("watcher") val watcher: Boolean,
)

/** 생성·수정 요청의 목표값 `{ key, value }`. */
@Serializable
data class ParamEntryRequest(
    @SerialName("key")
    val key: String,
    @SerialName("value")
    val value: String,
)

internal fun ParamEntry.toRequest(): ParamEntryRequest = ParamEntryRequest(key = key, value = value)
