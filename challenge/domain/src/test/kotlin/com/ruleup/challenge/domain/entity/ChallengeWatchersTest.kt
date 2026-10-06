package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 감시자 추가 버튼의 「N명 더 가능」 표기 근거. */
class ChallengeWatchersTest {
    @Test
    fun `해제되거나 만료된 감시자는 한도를 차지하지 않는다`() {
        val watchers =
            ChallengeWatchers(
                limit = 3,
                watchers =
                    listOf(
                        watcher(WatcherStatus.ACTIVE),
                        watcher(WatcherStatus.INVITED),
                        watcher(WatcherStatus.REVOKED),
                        watcher(WatcherStatus.EXPIRED),
                    ),
            )

        assertEquals(1, watchers.remaining)
    }

    @Test
    fun `한도를 넘겨 받아도 남은 수는 음수가 되지 않는다`() {
        val watchers = ChallengeWatchers(limit = 1, watchers = listOf(watcher(WatcherStatus.ACTIVE), watcher(WatcherStatus.ACTIVE)))

        assertEquals(0, watchers.remaining)
    }

    @Test
    fun `한도가 없으면 남은 수를 지어내지 않는다`() {
        assertNull(ChallengeWatchers(limit = null, watchers = listOf(watcher(WatcherStatus.ACTIVE))).remaining)
    }

    private fun watcher(status: WatcherStatus) =
        Watcher(
            watcherId = status.value,
            type = WatcherType.entries.first(),
            channel = null,
            status = status,
            displayName = "엄마",
            contactMasked = null,
            expiresAt = null,
            reinviteAvailableAt = null,
        )
}
