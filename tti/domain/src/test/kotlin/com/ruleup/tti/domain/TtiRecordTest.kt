package com.ruleup.tti.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 한 건이 언제 완성되고 합계가 어떻게 나오는지. **여기가 틀리면 안 끝난 측정이 나가거나, 끝난
 * 측정이 영영 안 나간다.**
 */
class TtiRecordTest {
    @Test
    fun `네 구간이 다 닫혀야 완성이다`() {
        // 하나라도 비면 쏘지 않고 저장소에 남긴다 — 반쪽짜리 숫자가 대시보드에 섞이면 안 된다.
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
        // 구간 사이에는 측정하지 않는 빈 시간이 끼어들 수 있다. 그것까지 TTI 로 세면 화면이
        // 느려진 것처럼 보인다.
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
        // 큰 사진이 없는 화면은 BIG_PART_LOADING 을 0ms 로 닫는다. 비워 두면 그 기록은 영영
        // 완성되지 않아 네 구간이 통째로 사라진다.
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
        // 늘리면 REQUIRED_COUNT 가 함께 움직여 예전 기록이 전부 미완성이 된다 — 의도한 변경인지
        // 여기서 한 번 걸러진다.
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
