package com.ruleup.notification.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.notification.data.dto.MarkReadRequest
import com.ruleup.notification.data.dto.NotificationPageResponse
import com.ruleup.notification.data.dto.NotificationSettingsRequest
import com.ruleup.notification.data.dto.NotificationSettingsResponse
import com.ruleup.notification.data.dto.NotificationSettingsUpdateResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    /** 알림 센터 목록. */
    @GET("v1/notifications")
    suspend fun getNotifications(
        @Query("tab") tab: String? = null,
        @Query("cursor") cursor: String? = null,
    ): BaseResponse<NotificationPageResponse>

    /** 읽음 지점 갱신. 204 라 [Response] 로 받는다. */
    @PUT("v1/notifications/read")
    suspend fun markRead(
        @Body request: MarkReadRequest,
    ): Response<BaseResponse<EmptyData>>

    @GET("v1/users/me/notification-settings")
    suspend fun getSettings(): BaseResponse<NotificationSettingsResponse>

    @PATCH("v1/users/me/notification-settings")
    suspend fun updateSettings(
        @Body request: NotificationSettingsRequest,
    ): BaseResponse<NotificationSettingsUpdateResponse>

    // 챌린지 음소거 등록. 204 라 Response 로 받는다
    @PUT("v1/users/me/notification-settings/mutes/{challengeId}")
    suspend fun mute(
        @Path("challengeId") challengeId: String,
    ): Response<BaseResponse<EmptyData>>

    // 챌린지 음소거 해제
    @DELETE("v1/users/me/notification-settings/mutes/{challengeId}")
    suspend fun unmute(
        @Path("challengeId") challengeId: String,
    ): Response<BaseResponse<EmptyData>>
}
