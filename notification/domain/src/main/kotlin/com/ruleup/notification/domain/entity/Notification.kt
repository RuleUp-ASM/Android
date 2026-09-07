package com.ruleup.notification.domain.entity

/**
 * 알림 센터의 한 줄 (명세: GET /notifications `items[]`).
 *
 * 알림은 **푸시 발송 여부·설정·시각과 무관하게 발생 즉시 적재된다** — [createdAt] 이 고지가
 * 성립한 시각이다. 그래서 푸시를 못 받았거나 야간에 눌린 알림도 여기엔 반드시 있다.
 *
 * [deeplink] 는 서버가 준 `ruleup://…` 문자열을 그대로 들고 있는다. 클라가 조립하지 않는다 —
 * 타입이 늘 때마다 앱을 고쳐야 하는 구조를 만들지 않기 위해서다.
 */
data class Notification(
    val id: String,
    val type: NotificationType?,
    val title: String,
    // 한 줄 요약. 없을 수 있다
    val body: String?,
    val deeplink: String?,
    // 챌린지에 귀속된 알림이면 그 방. 미읽음 카운터를 나누는 키다
    val challengeId: String?,
    // ISO-8601. 고지가 성립한 시각
    val createdAt: String,
)

/**
 * 알림 센터 한 페이지 (명세: GET /notifications).
 *
 * **미읽음은 서버가 세지 않는다.** 목록이 최신순이므로 [lastReadNotificationId] 에 해당하는
 * 항목보다 **위에 있는 것이 전부 미읽음**이다. id 크기를 비교하지 않고 목록에서의 위치로
 * 판단한다 — id 형식이 바뀌어도 판정이 흔들리지 않는다.
 *
 * [lastReadNotificationId] 가 이 페이지에 없으면 페이지 전체가 미읽음이고, 다음 페이지를 이어
 * 읽어야 경계를 만난다. 카운터 상한이 `99+` 라 최대 [MAX_UNREAD_PAGES] 장이면 충분하다.
 */
data class NotificationPage(
    val items: List<Notification>,
    val nextCursor: String?,
    // 보관 기간(일). 그보다 오래된 알림은 응답에 없다
    val retentionDays: Int?,
    val lastReadNotificationId: String?,
    /**
     * 서버가 세어 준 미읽음 수.
     *
     * 확정 명세는 이 필드를 폐기하고 클라 계산으로 옮겼지만, **배포된 서버는 아직 이 값을
     * 보내고 [lastReadNotificationId] 를 보내지 않는다**(2026-09-07 스테이징 확인).
     * 기준선이 없을 때만 쓰는 폴백이다 — 둘 다 "몇 개가 안 읽혔나"라는 같은 사실이다.
     */
    val serverUnreadCount: Int?,
) {
    /**
     * 이 페이지에서 읽지 않은 항목.
     *
     * 기준선이 이 페이지에 있으면 그 위까지가 미읽음이고, 없으면 전부 미읽음이다.
     * 기준선 자체가 null(한 번도 안 읽음)이어도 전부 미읽음이다.
     */
    val unread: List<Notification>
        get() {
            val boundary = lastReadNotificationId ?: return items
            val index = items.indexOfFirst { it.id == boundary }
            return if (index < 0) items else items.take(index)
        }

    /** 기준선을 이 페이지에서 만났는가 — 못 만났으면 다음 페이지까지 세어야 한다. */
    val boundaryFound: Boolean
        get() = lastReadNotificationId != null && items.any { it.id == lastReadNotificationId }

    /**
     * 읽음 처리에 보낼 id. **응답에 실제로 담겼던 최신 항목**이어야 한다.
     *
     * 서버가 현재 시각으로 갱신하면 조회와 갱신 사이에 적재된 알림이 화면에 뜬 적 없이 읽음
     * 처리되어 레드닷이 영영 안 뜬다. 그래서 클라가 id 를 지정한다.
     */
    val readMarker: String?
        get() = items.firstOrNull()?.id

    companion object {
        /**
         * 미읽음을 세려고 읽는 최대 페이지 수.
         *
         * 카운터 상한이 `99+` 이고 페이지가 50건 고정이라 두 장이면 100건 — 그 이상 세도 화면에
         * 쓸 데가 없다.
         */
        const val MAX_UNREAD_PAGES = 2

        /** 숫자 카운터 상한. 넘으면 `99+` 로 표기한다. */
        const val UNREAD_DISPLAY_CAP = 99
    }
}

/** 읽음 지점을 따로 보관하는 탭 (명세 `tab`). 공지를 읽었다고 알림이 읽음 처리되면 안 된다. */
enum class NotificationTab(
    val value: String,
) {
    NOTIFICATION("NOTIFICATION"),
    ANNOUNCEMENT("ANNOUNCEMENT"),
}

/**
 * 여러 페이지를 합친 미읽음 집계.
 *
 * 화면 두 곳이 이걸 나눠 쓴다 — 마이 진입점은 **레드닷**(숫자 없음), 챌린지 카드는
 * [challengeId] 별 **숫자 카운터**다.
 */
data class UnreadSummary(
    val total: Int,
    // challengeId → 미읽음 수. 챌린지에 귀속되지 않은 알림은 여기 없다
    val byChallenge: Map<String, Int>,
) {
    val hasUnread: Boolean
        get() = total > 0

    /** "3" / "99+" — 상한을 넘으면 정확한 수를 말하지 않는다. */
    fun badgeOf(challengeId: String): String? {
        val count = byChallenge[challengeId] ?: return null
        if (count <= 0) return null
        return if (count > NotificationPage.UNREAD_DISPLAY_CAP) "${NotificationPage.UNREAD_DISPLAY_CAP}+" else "$count"
    }

    companion object {
        val EMPTY = UnreadSummary(total = 0, byChallenge = emptyMap())
    }
}
