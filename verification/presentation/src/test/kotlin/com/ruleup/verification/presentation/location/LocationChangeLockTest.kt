package com.ruleup.verification.presentation.location

import com.ruleup.verification.presentation.location.viewmodel.VerificationLocationState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LocationChangeLockTest {
    @Test
    fun `최초 등록 중에는 잠금 안내가 붙지 않는다`() {
        // 첫 설정은 월 변경 횟수를 소진하지 않는다 — 잠길 이유가 없다.
        assertNull(state(isEditing = false, changeAvailable = true).changeLockNotice())
        assertNull(state(isEditing = false, changeAvailable = false).changeLockNotice())
    }

    @Test
    fun `이번 달 여유가 남았으면 안내가 없다`() {
        assertNull(state(isEditing = true, changeAvailable = true).changeLockNotice())
    }

    @Test
    fun `소진했으면 언제부터 가능한지 말한다`() {
        val notice =
            state(
                isEditing = true,
                changeAvailable = false,
                nextChangeAvailableAt = "2026-09-01T00:00:00+09:00",
            ).changeLockNotice()

        assertEquals("이번 달 변경 횟수를 모두 썼어요 · 9월 1일부터 가능해요", notice)
    }

    @Test
    fun `다음 가능일을 모르면 날짜를 지어내지 않는다`() {
        // 틀린 날짜를 확정처럼 보여주는 쪽이 아무 날짜도 없는 것보다 나쁘다.
        val notice = state(isEditing = true, changeAvailable = false, nextChangeAvailableAt = null).changeLockNotice()

        assertEquals("이번 달 변경 횟수를 모두 썼어요", notice)
    }

    private fun state(
        isEditing: Boolean,
        changeAvailable: Boolean,
        nextChangeAvailableAt: String? = null,
    ): VerificationLocationState =
        VerificationLocationState(
            isEditing = isEditing,
            changeAvailable = changeAvailable,
            nextChangeAvailableAt = nextChangeAvailableAt,
        )
}
