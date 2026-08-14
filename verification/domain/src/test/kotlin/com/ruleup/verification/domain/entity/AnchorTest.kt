package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LocationPinTest {
    @Test
    fun `범위를 벗어난 반경으로는 만들 수 없다`() {
        // 서버 판정 반경과 OS 트리거 반경이 어긋나면 안 되므로 생성 시점에 끊는다.
        assertFailsWith<IllegalArgumentException> { pin(radiusM = SetupAnchors.MAX_RADIUS_M + 1f) }
        assertFailsWith<IllegalArgumentException> { pin(radiusM = SetupAnchors.MIN_RADIUS_M - 1f) }
    }

    @Test
    fun `경계값은 통과한다`() {
        assertEquals(SetupAnchors.MIN_RADIUS_M, pin(radiusM = SetupAnchors.MIN_RADIUS_M).radiusM)
        assertEquals(SetupAnchors.MAX_RADIUS_M, pin(radiusM = SetupAnchors.MAX_RADIUS_M).radiusM)
    }
}

class AnchorSetTest {
    @Test
    fun `최대 개수를 넘으면 만들 수 없다`() {
        val tooMany = List(SetupAnchors.MAX_COUNT + 1) { pin() }

        assertFailsWith<IllegalArgumentException> { AnchorSet.of(tooMany) }
    }

    @Test
    fun `최대 개수까지는 통과한다`() {
        val pins = List(SetupAnchors.MAX_COUNT) { pin() }

        assertEquals(SetupAnchors.MAX_COUNT, AnchorSet.of(pins).pins.size)
    }

    @Test
    fun `앵커 없이도 만들 수 있다`() {
        // 앱 전용 셋업은 앵커 없이 제출하고 서버가 location 을 생략한다(명세 setup).
        assertTrue(AnchorSet.of(emptyList()).isEmpty)
        assertTrue(AnchorSet.EMPTY.isEmpty)
    }
}

private fun pin(radiusM: Float = SetupAnchors.MIN_RADIUS_M) = LocationPin(lat = 37.0, lng = 127.0, radiusM = radiusM, label = null)
