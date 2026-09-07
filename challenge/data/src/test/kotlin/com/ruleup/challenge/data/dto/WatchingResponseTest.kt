package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.WatcherStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 「내가 받는 알림」 매핑. 여기서 푸시 상태를 잘못 접으면 **설정 화면이 실제와 다른 말을 한다** —
 * 꺼진 것처럼 그렸는데 알림이 오면 사용자는 설정을 믿지 않게 된다.
 */
class WatchingResponseTest {
    @Test
    fun `식별자 없는 항목은 목록에 세우지 않는다`() {
        // 토글을 걸 대상이 없으면 끌 수 없는 스위치가 된다.
        val items = WatchingListResponse(items = listOf(WatchingItemResponse(watcherId = null))).toDomain()

        assertTrue(items.isEmpty())
    }

    @Test
    fun `푸시 수신 여부를 모르면 켜져 있다고 본다`() {
        val item = WatchingListResponse(items = listOf(WatchingItemResponse(watcherId = "w1", pushEnabled = null))).toDomain()

        assertEquals(true, item.single().pushEnabled)
    }

    @Test
    fun `모르는 관계 상태는 비워 두고 행은 남긴다`() {
        val item = WatchingListResponse(items = listOf(WatchingItemResponse(watcherId = "w1", status = "PAUSED"))).toDomain()

        assertEquals(null, item.single().status)
    }

    @Test
    fun `수신거부한 관계는 그 사실을 그대로 읽는다`() {
        val item = WatchingListResponse(items = listOf(WatchingItemResponse(watcherId = "w1", status = "REVOKED"))).toDomain()

        assertEquals(WatcherStatus.REVOKED, item.single().status)
        assertEquals(true, item.single().isRevoked)
    }

    @Test
    fun `수신 설정 응답에 식별자가 없으면 요청한 항목을 가리킨 것으로 본다`() {
        val update = WatchingUpdateResponse(watcherId = null, pushEnabled = false).toDomain(requestedId = "w1")

        assertEquals("w1", update.watcherId)
        assertEquals(false, update.pushEnabled)
    }
}

/**
 * 감시자 목록 응답의 키 이름.
 *
 * 명세는 `watchers` 인데 **실서버는 `items` 로 내린다**(2026-09-07 확인). 한쪽만 읽으면 목록이
 * 통째로 비어 화면이 "감시자가 없다"고 조용히 거짓말한다 — 지정해 둔 사람이 사라진 것처럼 보인다.
 */
class WatchersResponseKeyTest {
    @Test
    fun `서버가 items 로 내려도 목록을 읽는다`() {
        val watchers =
            WatchersResponse(items = listOf(WatcherResponse(watcherId = "w1", status = "ACTIVE"))).toDomain()

        assertEquals(listOf("w1"), watchers.watchers.map { it.watcherId })
    }

    @Test
    fun `명세대로 watchers 로 내려도 목록을 읽는다`() {
        val watchers =
            WatchersResponse(watchers = listOf(WatcherResponse(watcherId = "w1", status = "ACTIVE"))).toDomain()

        assertEquals(listOf("w1"), watchers.watchers.map { it.watcherId })
    }

    @Test
    fun `한도를 안 주면 무제한으로 본다`() {
        // 실제 응답에 slots 가 없다. 임의의 숫자를 넣으면 없는 제한을 화면이 지어낸다.
        val watchers = WatchersResponse(items = emptyList()).toDomain()

        assertEquals(null, watchers.limit)
    }

    @Test
    fun `구독 중이면 무료 한도가 와도 무제한으로 본다`() {
        val watchers =
            WatchersResponse(
                slots = WatcherSlotsResponse(used = 5, freeLimit = 3, subscribed = true),
                items = emptyList(),
            ).toDomain()

        assertEquals(null, watchers.limit)
    }
}
