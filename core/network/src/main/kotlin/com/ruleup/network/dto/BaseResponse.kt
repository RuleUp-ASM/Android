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
    // 429 등에서 재시도까지 남은 초.
    @SerialName("retryAfterSeconds")
    val retryAfterSeconds: Int? = null,
    // code 하나에 여러 원인이 묶인 계약(챌린지 가입의 409 JOIN_BLOCKED 등)에서 분기를 가르는 키.
    @SerialName("reason")
    val reason: String? = null,
    // REJOIN_COOLDOWN 계열의 재시도 가능 시각(ISO).
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
        // 서버가 429 의 대기 초를 두 자리에 섞어 보낸다 — 명시 필드가 없으면 `reason` 에 숫자만
        // 담아 온다. 명시 필드만 읽으면 **대기 시간을 0 으로 읽어 버튼이 즉시 다시 열린다**(CRE-04).
        // `reason` 이 분기 키(JOIN_BLOCKED 계열)일 때는 숫자가 아니라 null 로 떨어져 영향이 없다.
        retryAfterSeconds = this?.retryAfterSeconds ?: this?.reason?.trim()?.toIntOrNull(),
        reason = this?.reason,
        rejoinAvailableAt = this?.rejoinAvailableAt,
    )

class ApiException(
    val code: String,
    message: String,
    val retryAfterSeconds: Int? = null,
    // ErrorBody.reason 그대로 — code 안에서 분기를 가르는 키.
    val reason: String? = null,
    val rejoinAvailableAt: String? = null,
) : Exception(message)

fun <T> T?.requireField(field: String): T =
    this ?: throw ApiException(
        code = "RESPONSE_FIELD_MISSING",
        message = "필수 응답 필드 누락: $field",
    )
