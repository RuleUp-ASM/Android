package com.ruleup.verification.presentation.location.viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * [PendingSelection.category] (신규, 검색 결과의 카카오 로컬 카테고리 표시용) 와
 * [VerificationLocationIntent.Back] (신규, 검색 필 뒤로가기) 계약을 검증한다.
 * 나머지 필드/인텐트는 이번 PR 에서 변경되지 않았으므로 다루지 않는다.
 */
class VerificationLocationContractTest {
    @Test
    fun `category 를 생략하면 기본값은 null 이다`() {
        val pending = PendingSelection(lat = 37.0, lng = 127.0, name = "헬스장", address = "서울시")

        assertNull(pending.category)
    }

    @Test
    fun `category 를 지정하면 그대로 보관된다`() {
        val pending =
            PendingSelection(
                lat = 37.0,
                lng = 127.0,
                name = "헬스장",
                address = "서울시",
                category = "스포츠,레저 > 헬스장",
            )

        assertEquals("스포츠,레저 > 헬스장", pending.category)
    }

    @Test
    fun `category 만 다르면 동등하지 않다 - data class 계약`() {
        val withCategory = PendingSelection(37.0, 127.0, "헬스장", "서울시", category = "헬스장")
        val withoutCategory = PendingSelection(37.0, 127.0, "헬스장", "서울시", category = null)

        assertNotEquals(withCategory, withoutCategory)
    }

    @Test
    fun `Back 은 데이터를 담지 않는 싱글턴 인텐트다`() {
        val first: VerificationLocationIntent = VerificationLocationIntent.Back
        val second: VerificationLocationIntent = VerificationLocationIntent.Back

        assertEquals(first, second)
    }
}