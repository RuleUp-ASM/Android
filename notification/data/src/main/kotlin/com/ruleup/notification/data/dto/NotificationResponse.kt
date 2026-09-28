package com.ruleup.notification.data.dto

import com.ruleup.notification.domain.entity.Notification
import com.ruleup.notification.domain.entity.NotificationPage
import com.ruleup.notification.domain.entity.NotificationType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 알림 센터 목록 (GET /notifications)
@Serializable
data class NotificationItemResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("body")
    val body: String? = null,
    // ruleup://… 커스텀 스킴.
    @SerialName("deeplink")
    val deeplink: String? = null,
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

@Serializable
data class NotificationPageResponse(
    @SerialName("items")
    val items: List<NotificationItemResponse>? = null,
    @SerialName("nextCursor")
    val nextCursor: String? = null,
    @SerialName("retentionDays")
    val retentionDays: Int? = null,
    // 미읽음 판정의 기준선.
    @SerialName("lastReadNotificationId")
    val lastReadNotificationId: String? = null,
    // 폐기 예정.
    @SerialName("unreadCount")
    val unreadCount: Int? = null,
)

internal fun NotificationPageResponse.toDomain(): NotificationPage =
    NotificationPage(
        // 식별자·시각이 없는 알림은 세울 자리가 없다
        items = items.orEmpty().mapNotNull { it.toDomain() },
        nextCursor = nextCursor,
        retentionDays = retentionDays,
        lastReadNotificationId = lastReadNotificationId,
        serverUnreadCount = unreadCount,
    )

internal fun NotificationItemResponse.toDomain(): Notification? {
    val id = id ?: return null
    val createdAt = createdAt ?: return null
    return Notification(
        id = id,
        // 모르는 타입도 목록에는 세운다
        type = NotificationType.fromValue(type),
        title = title.orEmpty(),
        body = body?.takeIf { it.isNotBlank() },
        deeplink = deeplink?.takeIf { it.isNotBlank() },
        challengeId = challengeId,
        createdAt = createdAt,
    )
}

// 읽음 처리 (PUT /notifications/read)
@Serializable
data class MarkReadRequest(
    // NOTIFICATION / ANNOUNCEMENT
    @SerialName("tab")
    val tab: String,
    // 응답에 실제로 담겼던 최신 항목의 id.
    @SerialName("lastNotificationId")
    val lastNotificationId: String,
)

/** 응답 본문이 없는 계약이지만(204), 서버가 봉투를 실어 보내도 읽히도록 최소 필드만 둔다. */
@Serializable
class MarkReadResponse
