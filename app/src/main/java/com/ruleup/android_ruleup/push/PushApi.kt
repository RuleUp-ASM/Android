package com.ruleup.android_ruleup.push

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.HTTP
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

@Serializable
data class UnregisterDeviceRequest(
    // 해제할 FCM 토큰 (필수, 공백 불가). 삭제 범위는 (현재 유저, 이 토큰) 조합.
    @SerialName("token")
    val token: String,
)

interface PushApi {
    // FCM 디바이스 토큰 등록 (명세: POST /api/v1/devices — 멱등 upsert, 죽은 토큰은 서버가 자동 정리)
    @POST("v1/devices")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest,
    ): BaseResponse<EmptyData>

    // FCM 디바이스 토큰 폐기 (명세: DELETE /api/v1/devices — 로그아웃/폐기 시, 멱등).
    // DELETE + 바디라 @HTTP(hasBody=true) 를 쓴다(@DELETE 는 바디 미지원).
    @HTTP(method = "DELETE", path = "v1/devices", hasBody = true)
    suspend fun unregisterDevice(
        @Body request: UnregisterDeviceRequest,
    ): BaseResponse<EmptyData>
}
