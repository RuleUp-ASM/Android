package com.ruleup.tti.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 한 건이 언제 완성되고 합계가 어떻게 나오는지. */
class TtiRecordTest {
    @Test
    fun `네 구간이 다 닫혀야 완성이다`() {
        assertTrue(record(closed = TtiTimeline.entries).isComplete)
        assertFalse(record(closed = TtiTimeline.entries.drop(1)).isComplete)
    }

    @Test
    fun `열려만 있고 안 닫힌 구간이 있으면 미완성이다`() {
        val open =
            record(closed = listOf(TtiTimeline.VIEW_CREATE)).copy(
                spans =
                    mapOf(
                        TtiTimeline.VIEW_CREATE to TtiSpan(startedAt = 0, endedAt = 10),
                        TtiTimeline.BACKEND to TtiSpan(startedAt = 10, endedAt = null),
                    ),
            )

        assertNull(open.totalTimeMillis)
        assertFalse(open.isComplete)
    }

    @Test
    fun `합계는 구간 길이의 합이지 처음과 끝의 차이가 아니다`() {
        val spaced =
            TtiRecord(
                tti = Tti("a"),
                pageName = "challenge_detail",
                createdAt = 0,
                spans =
                    mapOf(
                        TtiTimeline.VIEW_CREATE to TtiSpan(startedAt = 0, endedAt = 10),
                        // 10~1000 사이 990ms 는 사용자가 가만히 있던 시간이다
                        TtiTimeline.BACKEND to TtiSpan(startedAt = 1_000, endedAt = 1_050),
                        TtiTimeline.VIEW_BINDING to TtiSpan(startedAt = 1_050, endedAt = 1_070),
                        TtiTimeline.BIG_PART_LOADING to TtiSpan(startedAt = 1_070, endedAt = 1_090),
                    ),
            )

        assertEquals(100, spaced.totalTimeMillis)
    }

    @Test
    fun `길이 0 인 구간도 닫힌 것으로 센다`() {
        val skipped =
            record(closed = TtiTimeline.entries).copy(
                spans =
                    TtiTimeline.entries.associateWith { timeline ->
                        if (timeline == TtiTimeline.BIG_PART_LOADING) {
                            TtiSpan(startedAt = 30, endedAt = 30)
                        } else {
                            TtiSpan(startedAt = 0, endedAt = 10)
                        }
                    },
            )

        assertTrue(skipped.isComplete)
        assertEquals(30, skipped.totalTimeMillis)
    }

    @Test
    fun `구간은 네 종류뿐이다`() {
        assertEquals(
            listOf("VIEW_CREATE", "BACKEND", "VIEW_BINDING", "BIG_PART_LOADING"),
            TtiTimeline.entries.map { it.name },
        )
        assertEquals(4, TtiTimeline.REQUIRED_COUNT)
    }

    private fun record(closed: List<TtiTimeline>) =
        TtiRecord(
            tti = Tti("a"),
            pageName = "challenge_detail",
            createdAt = 0,
            spans = closed.associateWith { TtiSpan(startedAt = 0, endedAt = 10) },
        )
}
