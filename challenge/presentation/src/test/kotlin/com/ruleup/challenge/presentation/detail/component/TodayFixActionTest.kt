package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.FailureReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 실패 사유에서 그 사유를 푸는 화면으로 가는 경로.
 *
 * **누를 수 있는데 아무 일도 안 일어나는 버튼을 만들지 않는 것**이 이 매핑의 목적이다. 이미 지나간
 * 측정(걸음·수면)은 화면으로 되돌릴 수 없어 이의 제기가 맡고, 설정이 빠진 것만 여기서 고친다.
 */
class TodayFixActionTest {
    @Test
    fun `장소를 등록하지 않아 실패했으면 등록 화면으로 보낸다`() {
        val action = FailureReason.GEOFENCE_NOT_CONFIGURED.fixAction(anchor, repair, manual)

        assertEquals("인증 장소 등록하기", action?.label)
    }

    @Test
    fun `권한이 꺼져 실패했으면 권한 복구로 보낸다`() {
        // 장소 등록과 다른 화면이다 — 둘을 한 버튼으로 묶으면 사용자가 엉뚱한 설정을 헤맨다.
        val action = FailureReason.PERMISSION_MISSING.fixAction(anchor, repair, manual)

        assertEquals("권한 다시 연결하기", action?.label)
    }

    @Test
    fun `수동 체크를 놓쳤으면 체크 화면으로 보낸다`() {
        val action = FailureReason.MANUAL_NOT_SUBMITTED.fixAction(anchor, repair, manual)

        assertEquals("오늘 체크하기", action?.label)
    }

    @Test
    fun `지금 할 수 있는 조치가 없는 사유에는 버튼을 두지 않는다`() {
        // 이미 지난 측정이라 화면으로 되돌릴 수 없다. 버튼을 두면 눌러보고 고장이라 읽는다.
        assertNull(FailureReason.INSUFFICIENT_STEPS.fixAction(anchor, repair, manual))
        assertNull(FailureReason.WOKE_UP_LATE.fixAction(anchor, repair, manual))
        assertNull(FailureReason.UNKNOWN.fixAction(anchor, repair, manual))
    }

    @Test
    fun `그 방에 없는 진입점은 버튼으로 만들지 않는다`() {
        // 자동 방에는 수동 체크가, 앵커를 안 쓰는 방에는 장소 등록이 넘어오지 않는다.
        assertNull(FailureReason.GEOFENCE_NOT_CONFIGURED.fixAction(null, repair, manual))
        assertNull(FailureReason.MANUAL_NOT_SUBMITTED.fixAction(anchor, repair, null))
    }

    private val anchor: () -> Unit = {}
    private val repair: () -> Unit = {}
    private val manual: () -> Unit = {}
}
