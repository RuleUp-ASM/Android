package com.ruleup.notification.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.notification.data.dto.MarkReadRequest
import com.ruleup.notification.data.dto.NotificationPageResponse
import com.ruleup.notification.data.dto.NotificationSettingsRequest
import com.ruleup.notification.data.dto.NotificationSettingsResponse
import com.ruleup.notification.data.dto.NotificationSettingsUpdateResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    /**
     * 알림 센터 목록. **`size` 를 보내지 않는다** — 서버가 50으로 고정했고 요청 파라미터로 받지
     * 않는다(명세 2026-09-07).
     *
     * [tab] 은 알림(`NOTIFICATION`)과 운영자 공지(`ANNOUNCEMENT`)를 가른다. 공지를 알림 레코드로
     * 흡수하기로 확정되면서 구 `GET /announcements` 가 폐기되고 이 필터로 통합됐다(2026-09-07).
     * 읽음 지점도 탭별로 따로 보관되므로 응답의 `lastReadNotificationId` 는 **요청한 탭의 값**이다.
     */
    @GET("v1/notifications")
    suspend fun getNotifications(
        @Query("tab") tab: String? = null,
        @Query("cursor") cursor: String? = null,
    ): BaseResponse<NotificationPageResponse>

    /**
     * 읽음 지점 갱신.
     *
     * 반환형이 nullable 인 이유 — 성공이 **204(본문 없음)** 라 Retrofit 이 본문을 null 로 준다.
     * 그렇다고 `Unit` 으로 두면 실패 응답의 봉투를 못 읽어 오류가 조용히 삼켜진다.
     */
    @PUT("v1/notifications/read")
    suspend fun markRead(
        @Body request: MarkReadRequest,
    ): BaseResponse<EmptyData>?

    @GET("v1/users/me/notification-settings")
    suspend fun getSettings(): BaseResponse<NotificationSettingsResponse>

    @PATCH("v1/users/me/notification-settings")
    suspend fun updateSettings(
        @Body request: NotificationSettingsRequest,
    ): BaseResponse<NotificationSettingsUpdateResponse>

    // 챌린지 음소거 등록 — 멱등 204. nullable 인 이유는 markRead 와 같다
    @PUT("v1/users/me/notification-settings/mutes/{challengeId}")
    suspend fun mute(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<EmptyData>?

    // 챌린지 음소거 해제 — 멱등 204
    @DELETE("v1/users/me/notification-settings/mutes/{challengeId}")
    suspend fun unmute(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<EmptyData>?
}
