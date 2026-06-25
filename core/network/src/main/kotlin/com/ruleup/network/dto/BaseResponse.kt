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
)

fun <T> BaseResponse<T>.getOrThrow(): T =
    if (success && data != null) {
        data
    } else {
        throw ApiException(
            code = error?.code ?: "UNKNOWN",
            message = error?.message ?: "Unknown Error",
        )
    }

fun BaseResponse<*>.throwOnError() {
    if (!success) {
        throw ApiException(
            code = error?.code ?: "UNKNOWN",
            message = error?.message ?: "Unknown Error",
        )
    }
}

class ApiException(
    val code: String,
    message: String,
) : Exception(message)

fun <T> T?.requireField(field: String): T =
    this ?: throw ApiException(
        code = "RESPONSE_FIELD_MISSING",
        message = "필수 응답 필드 누락: $field",
    )
