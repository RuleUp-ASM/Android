package com.ruleup.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    @SerialName("success")
    val success: Boolean,
    @SerialName("data")
    val data: T? = null,
    @SerialName("error")
    val error: ErrorBody? = null,
)

/** `data` 가 없는(`success: true` 만 오는) 응답용 빈 페이로드. */
@Serializable
class EmptyData

@Serializable
data class ErrorBody(
    @SerialName("code")
    val code: String,
    @SerialName("message")
    val message: String,
    // rate limit(429) 등에서 재시도까지 남은 초. 없으면 null.
    @SerialName("retryAfterSeconds")
    val retryAfterSeconds: Int? = null,
    // 같은 code 안에서 분기를 가르는 세부 사유. 챌린지 가입의 409 JOIN_BLOCKED 처럼 코드 하나에
    // 여러 원인이 묶인 계약에서 쓴다.
    @SerialName("reason")
    val reason: String? = null,
    // REJOIN_COOLDOWN 계열에서 재시도 가능 시각(ISO). 없으면 null.
    @SerialName("rejoinAvailableAt")
    val rejoinAvailableAt: String? = null,
)

fun <T> BaseResponse<T>.getOrThrow(): T =
    if (success && data != null) {
        data
    } else {
        throw error.toException()
    }

fun BaseResponse<*>.throwOnError() {
    if (!success) {
        throw error.toException()
    }
}

private fun ErrorBody?.toException(): ApiException =
    ApiException(
        code = this?.code ?: "UNKNOWN",
        message = this?.message ?: "Unknown Error",
        retryAfterSeconds = this?.retryAfterSeconds,
        reason = this?.reason,
        rejoinAvailableAt = this?.rejoinAvailableAt,
    )

class ApiException(
    val code: String,
    message: String,
    val retryAfterSeconds: Int? = null,
    // ErrorBody.reason 과 같은 의미 — code 하나에 여러 원인이 묶인 계약의 분기 키.
    val reason: String? = null,
    val rejoinAvailableAt: String? = null,
) : Exception(message)

fun <T> T?.requireField(field: String): T =
    this ?: throw ApiException(
        code = "RESPONSE_FIELD_MISSING",
        message = "필수 응답 필드 누락: $field",
    )
