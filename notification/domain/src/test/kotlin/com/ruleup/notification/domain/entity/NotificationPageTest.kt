package com.ruleup.notification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 미읽음 판정. **서버가 세어 주지 않으므로 이 규칙이 곧 레드닷과 카운터의 정의**다.
 *
 * 목록은 최신순이라 기준선보다 **위에 있는 것이 미읽음**이다 — id 크기를 비교하지 않는다.
 * 크기로 비교하면 id 형식이 바뀌는 순간(UUIDv7 → 다른 체계) 판정이 통째로 뒤집힌다.
 */
class NotificationPageTest {
    @Test
    fun `기준선보다 위에 있는 항목만 읽지 않은 것으로 본다`() {
        val page = page(listOf("n3", "n2", "n1"), lastRead = "n2")

        assertEquals(listOf("n3"), page.unread.map { it.id })
    }

    @Test
    fun `기준선이 없으면 전부 읽지 않은 것이다`() {
        // 한 번도 알림 센터에 들어간 적 없는 사용자다.
        val page = page(listOf("n2", "n1"), lastRead = null)

        assertEquals(listOf("n2", "n1"), page.unread.map { it.id })
    }

    @Test
    fun `기준선이 이 페이지에 없으면 전부 미읽음이고 다음 장을 더 읽어야 한다`() {
        // 기준선이 뒤 페이지에 있다는 뜻이다 — 여기서 멈추면 미읽음을 덜 센다.
        val page = page(listOf("n9", "n8"), lastRead = "n1")

        assertEquals(2, page.unread.size)
        assertFalse(page.boundaryFound)
    }

    @Test
    fun `기준선을 만나면 더 읽지 않는다`() {
        val page = page(listOf("n3", "n2"), lastRead = "n2")

        assertTrue(page.boundaryFound)
    }

    @Test
    fun `읽음 처리에는 응답에 담겼던 최신 id 를 쓴다`() {
        // 현재 시각으로 갱신하면 조회와 갱신 사이에 적재된 알림이 화면에 뜬 적 없이 읽음 처리된다.
        val page = page(listOf("n3", "n2", "n1"), lastRead = null)

        assertEquals("n3", page.readMarker)
    }

    @Test
    fun `목록이 비면 읽음 처리에 보낼 값이 없다`() {
        val page = page(emptyList(), lastRead = null)

        assertNull(page.readMarker)
    }

    private fun page(
        ids: List<String>,
        lastRead: String?,
    ) = NotificationPage(
        items =
            ids.map {
                Notification(
                    id = it,
                    type = null,
                    title = it,
                    body = null,
                    deeplink = null,
                    challengeId = null,
                    createdAt = "2026-09-04T00:00:00+09:00",
                )
            },
        nextCursor = null,
        retentionDays = 180,
        lastReadNotificationId = lastRead,
        serverUnreadCount = null,
    )
}

/**
 * 미읽음 집계의 표시 규칙.
 *
 * 상한이 `99+` 라 **정확한 수를 말하지 않는 구간**이 있다. 100건을 "100"으로 쓰면 클라가 100건을
 * 다 세야 한다는 뜻이 되고, 그러면 페이지를 무한정 읽게 된다.
 */
class UnreadSummaryTest {
    @Test
    fun `미읽음이 없는 챌린지에는 뱃지를 붙이지 않는다`() {
        val summary = UnreadSummary(total = 0, byChallenge = emptyMap())

        assertNull(summary.badgeOf("c1"))
        assertFalse(summary.hasUnread)
    }

    @Test
    fun `상한 이하면 정확한 수를 보여 준다`() {
        val summary = UnreadSummary(total = 99, byChallenge = mapOf("c1" to 99))

        assertEquals("99", summary.badgeOf("c1"))
    }

    @Test
    fun `상한을 넘으면 정확한 수를 말하지 않는다`() {
        // 100 을 그대로 쓰면 클라가 100건을 다 세야 한다 — 그래서 상한이 있는 것이다.
        val summary = UnreadSummary(total = 100, byChallenge = mapOf("c1" to 100))

        assertEquals("99+", summary.badgeOf("c1"))
    }
}

/**
 * 알림 타입 레지스트리.
 *
 * **모르는 타입도 목록에 세워야 한다** — 서버가 타입을 늘리는 것은 정상이고, 그때 적재된 고지가
 * 화면에서 사라지면 법적 고지가 성립한 항목을 사용자가 못 보게 된다.
 */
class NotificationTypeTest {
    @Test
    fun `모르는 타입은 목록에서 지우지 않고 분류만 비운다`() {
        assertNull(NotificationType.fromValue("SOMETHING_NEW"))
    }

    @Test
    fun `제재 고지는 계정 그룹이라 계정 토글로 꺼진다`() {
        // "끌 수 없는 푸시는 없다" — 고지 의무는 알림 센터 적재로 충족된다(테크 스펙 4).
        assertEquals(NotificationGroup.ACCOUNT, NotificationType.KICK_CONFIRMED.group)
        assertEquals(NotificationGroup.ACCOUNT, NotificationType.ACCOUNT_LOCK_CHANGED.group)
    }

    @Test
    fun `루틴 리마인더만 그룹 토글이 없는 상시 알림이다`() {
        // 리마인더를 챌린지 그룹에 넣으면 끌 수 없어야 할 토글이 하나 생긴다.
        val alwaysOn = NotificationType.entries.filter { it.group == NotificationGroup.REMINDER }

        assertEquals(listOf(NotificationType.ROUTINE_REMINDER), alwaysOn)
    }
}
