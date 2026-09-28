package com.ruleup.notification.domain.entity

/** 알림 센터의 한 줄. */
data class Notification(
    val id: String,
    val type: NotificationType?,
    val title: String,
    // 한 줄 요약.
    val body: String?,
    val deeplink: String?,
    // 챌린지에 귀속된 알림이면 그 방.
    val challengeId: String?,
    // ISO-8601.
    val createdAt: String,
)

/** 알림 센터 한 페이지. */
data class NotificationPage(
    val items: List<Notification>,
    val nextCursor: String?,
    // 보관 기간(일).
    val retentionDays: Int?,
    val lastReadNotificationId: String?,
    /** 서버가 세어 준 미읽음 수. */
    val serverUnreadCount: Int?,
) {
    /** 이 페이지에서 읽지 않은 항목. */
    val unread: List<Notification>
        get() {
            val boundary = lastReadNotificationId ?: return items
            val index = items.indexOfFirst { it.id == boundary }
            return if (index < 0) items else items.take(index)
        }

    /** 기준선을 이 페이지에서 만났는가 */
    val boundaryFound: Boolean
        get() = lastReadNotificationId != null && items.any { it.id == lastReadNotificationId }

    /** 읽음 처리에 보낼 id. */
    val readMarker: String?
        get() = items.firstOrNull()?.id

    companion object {
        /** 미읽음을 세려고 읽는 최대 페이지 수. */
        const val MAX_UNREAD_PAGES = 2

        /** 숫자 카운터 상한. */
        const val UNREAD_DISPLAY_CAP = 99
    }
}

/** 읽음 지점을 따로 보관하는 탭. */
enum class NotificationTab(
    val value: String,
) {
    NOTIFICATION("NOTIFICATION"),
    ANNOUNCEMENT("ANNOUNCEMENT"),
}

/** 여러 페이지를 합친 미읽음 집계. */
data class UnreadSummary(
    val total: Int,
    // challengeId → 미읽음 수.
    val byChallenge: Map<String, Int>,
) {
    val hasUnread: Boolean
        get() = total > 0

    /** "3" / "99+" */
    fun badgeOf(challengeId: String): String? {
        val count = byChallenge[challengeId] ?: return null
        if (count <= 0) return null
        return if (count > NotificationPage.UNREAD_DISPLAY_CAP) "${NotificationPage.UNREAD_DISPLAY_CAP}+" else "$count"
    }

    companion object {
        val EMPTY = UnreadSummary(total = 0, byChallenge = emptyMap())
    }
}
