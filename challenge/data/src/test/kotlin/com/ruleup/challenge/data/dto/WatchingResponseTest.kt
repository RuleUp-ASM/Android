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
