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
    // FCM 등록 토큰 (필수, 공백 불가)
    @SerialName("token")
    val token: String,
    // 생략 시 서버 기본 ANDROID
    @SerialName("platform")
    val platform: String = "ANDROID",
)

@Serializable
data class UnregisterDeviceRequest(
    // 해제할 FCM 토큰 (필수, 공백 불가).
    @SerialName("token")
    val token: String,
)

interface PushApi {
    // FCM 디바이스 토큰 등록
    @POST("v1/devices")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest,
    ): BaseResponse<EmptyData>

    // FCM 디바이스 토큰 폐기.
    @HTTP(method = "DELETE", path = "v1/devices", hasBody = true)
    suspend fun unregisterDevice(
        @Body request: UnregisterDeviceRequest,
    ): BaseResponse<EmptyData>
}
