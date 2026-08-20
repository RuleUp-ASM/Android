package com.ruleup.verification.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun `기준 장소는 최대 3개다`() {
        // 인증 정책 §1.1 — 명세도 4개째부터 ANCHOR_LIMIT_EXCEEDED 로 막는다.
        assertEquals(3, SetupAnchors.MAX_COUNT)
    }

    @Test
    fun `앵커 없이도 만들 수 있다`() {
        // 앱 전용 셋업은 앵커 없이 제출하고 서버가 location 을 생략한다(명세 setup).
        assertTrue(AnchorSet.of(emptyList()).isEmpty)
        assertTrue(AnchorSet.EMPTY.isEmpty)
    }
}

private fun pin() = LocationPin(lat = 37.0, lng = 127.0, label = null)
