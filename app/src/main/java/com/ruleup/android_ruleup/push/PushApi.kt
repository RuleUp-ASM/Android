package com.ruleup.android_ruleup.push

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class RegisterDeviceRequest(
    // FCM 등록 토큰 (필수, 공백 불가) — 토큰 자체가 upsert 키(같은 토큰은 현재 유저로 재바인딩)
    @SerialName("token")
    val token: String,
    // 생략 시 서버 기본 ANDROID — 명시해 iOS 확장에 대비
    @SerialName("platform")
    val platform: String = "ANDROID",
)

interface PushApi {
    // FCM 디바이스 토큰 등록 (명세: POST /api/v1/devices — 멱등 upsert, 죽은 토큰은 서버가 자동 정리)
    @POST("v1/devices")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest,
    ): BaseResponse<EmptyData>
}
