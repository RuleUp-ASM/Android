package com.ruleup.notification.data.dto

import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 알림 목록 매핑. */
class NotificationResponseTest {
    @Test
    fun `모르는 타입도 목록에 남기고 분류만 비운다`() {
        val page =
            NotificationPageResponse(
                items = listOf(NotificationItemResponse(id = "n1", type = "SOMETHING_NEW", createdAt = "2026-09-04T00:00:00+09:00")),
            ).toDomain()

        assertEquals(1, page.items.size)
        assertNull(page.items.single().type)
    }

    @Test
    fun `식별자나 시각이 없는 알림은 세우지 않는다`() {
        // 읽음 기준선을 잡을 수도, 정렬할 수도 없다.
        val page =
            NotificationPageResponse(
                items =
                    listOf(
                        NotificationItemResponse(id = null, createdAt = "2026-09-04T00:00:00+09:00"),
                        NotificationItemResponse(id = "n1", createdAt = null),
                    ),
            ).toDomain()

        assertTrue(page.items.isEmpty())
    }

    @Test
    fun `기준선을 안 주는 서버에서는 서버 미읽음 수를 폴백으로 들고 온다`() {
        val page = NotificationPageResponse(items = emptyList(), unreadCount = 3).toDomain()

        assertNull(page.lastReadNotificationId)
        assertEquals(3, page.serverUnreadCount)
    }

    @Test
    fun `빈 본문은 없는 것으로 본다`() {
        // 빈 줄을 그리면 카드에 의미 없는 여백만 생긴다.
        val page =
            NotificationPageResponse(
                items = listOf(NotificationItemResponse(id = "n1", body = "  ", createdAt = "2026-09-04T00:00:00+09:00")),
            ).toDomain()

        assertNull(page.items.single().body)
    }
}

/** 설정 매핑. */
class NotificationSettingsResponseTest {
    @Test
    fun `설정을 통째로 안 주면 전부 켜진 것으로 본다`() {
        val settings = NotificationSettingsResponse().toDomain()

        assertTrue(settings.pushEnabled)
        assertTrue(settings.groups.account)
        assertTrue(settings.mutedChallengeIds.isEmpty())
    }

    @Test
    fun `groups 가 없으면 구 계약의 평평한 마케팅 값을 쓴다`() {
        val settings = NotificationSettingsResponse(legacyMarketing = false).toDomain()

        assertEquals(false, settings.groups.marketing)
    }

    @Test
    fun `마스터가 꺼져 있으면 그룹이 켜져 있어도 안 나가는 것으로 본다`() {
        // 가장 제한적인 것이 이긴다.
        val settings =
            NotificationSettingsResponse(
                pushEnabled = false,
                groups = NotificationGroupsResponse(account = true, challenge = true, marketing = true),
            ).toDomain()

        assertEquals(false, settings.effectivelyOn(NotificationGroup.ACCOUNT))
    }

    @Test
    fun `그룹을 하나도 안 바꾸면 groups 키 자체를 보내지 않는다`() {
        val request = NotificationSettingsUpdate(pushEnabled = false).toRequest()

        assertNull(request.groups)
        assertEquals(false, request.pushEnabled)
    }

    @Test
    fun `바꾸는 그룹만 실어 보낸다`() {
        val request = NotificationSettingsUpdate(marketing = false).toRequest()

        assertEquals(false, request.groups?.marketing)
        assertNull(request.groups?.account)
        assertNull(request.pushEnabled)
    }

    @Test
    fun `구 계약처럼 봉투 없이 설정만 와도 반영값을 읽는다`() {
        val result = NotificationSettingsUpdateResponse(flatMarketing = false).toDomain()

        assertEquals(false, result.settings.groups.marketing)
    }
}
