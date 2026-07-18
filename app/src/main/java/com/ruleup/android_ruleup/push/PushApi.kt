package com.ruleup.android_ruleup.push

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

@Serializable
data class RegisterFcmTokenRequest(
    @SerialName("fcmToken")
    val fcmToken: String,
    // uuid or ssaid — 기기 1대 = 토큰 1개 upsert 키
    @SerialName("deviceIdentifier")
    val deviceIdentifier: String,
)

interface PushApi {
    /**
     * FCM 토큰 저장 (명세: POST /users/fcm-token — 201, 본문 없음).
     * 응답이 공통 BaseResponse 래퍼가 아니라(빈 본문) Response 로 받아 상태 코드만 본다.
     */
    @POST("v1/users/fcm-token")
    suspend fun registerFcmToken(
        @Body request: RegisterFcmTokenRequest,
    ): Response<Unit>
}
