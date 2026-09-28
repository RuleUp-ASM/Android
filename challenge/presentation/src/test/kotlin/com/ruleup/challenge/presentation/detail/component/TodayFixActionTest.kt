package com.ruleup.challenge.presentation.detail.component

import com.ruleup.verification.domain.entity.FailureReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 실패 사유에서 그 사유를 푸는 화면으로 가는 경로. */
class TodayFixActionTest {
    @Test
    fun `장소를 등록하지 않아 실패했으면 등록 화면으로 보낸다`() {
        val action = FailureReason.GEOFENCE_NOT_CONFIGURED.fixAction(anchor, repair, manual)

        assertEquals("인증 장소 등록하기", action?.label)
    }

    @Test
    fun `권한이 꺼져 실패했으면 권한 복구로 보낸다`() {
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
