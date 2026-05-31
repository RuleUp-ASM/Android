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
            message = error?.message ?: "Unknown error",
        )
    }

/**
 * 본문 데이터가 없는 응답(예: 로그아웃, 삭제)용. 실패면 던지고, 성공이면 아무것도 반환하지 않는다.
 */
fun BaseResponse<*>.throwOnError() {
    if (!success) {
        throw ApiException(
            code = error?.code ?: "UNKNOWN",
            message = error?.message ?: "Unknown error",
        )
    }
}

class ApiException(
    val code: String,
    message: String,
) : Exception(message)

/**
 * 백엔드가 nullable 로 내려준 값 중, 반드시 필요한 필드가 비어 있을 때 던진다.
 */
fun <T> T?.requireField(field: String): T =
    this ?: throw ApiException(
        code = "RESPONSE_FIELD_MISSING",
        message = "필수 응답 필드 누락: $field",
    )
