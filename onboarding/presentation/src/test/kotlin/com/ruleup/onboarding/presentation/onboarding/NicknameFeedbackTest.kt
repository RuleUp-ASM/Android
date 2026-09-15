package com.ruleup.onboarding.presentation.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 닉네임 안내 한 줄. 형식 검사와 서버 확인을 따로 그리면 같은 안내가 두 번 뜬다. */
class NicknameFeedbackTest {
    @Test
    fun `입력이 없으면 안내를 띄우지 않는다`() {
        assertNull(nicknameFeedback("", serverMessage = "사용 가능한 닉네임이에요", serverAvailable = true))
    }

    @Test
    fun `형식이 틀리면 서버 결과보다 형식 안내를 먼저 보여 준다`() {
        val feedback = nicknameFeedback("가", serverMessage = "사용 가능한 닉네임이에요", serverAvailable = true)

        assertEquals(false, feedback?.positive)
    }

    @Test
    fun `형식이 맞으면 서버 확인 결과를 그대로 보여 준다`() {
        val feedback = nicknameFeedback("지현", serverMessage = "이미 사용 중인 닉네임이에요", serverAvailable = false)

        assertEquals(NicknameFeedback("이미 사용 중인 닉네임이에요", positive = false), feedback)
    }

    @Test
    fun `형식이 맞아도 서버 확인 전이면 안내를 지어내지 않는다`() {
        assertNull(nicknameFeedback("지현", serverMessage = null, serverAvailable = null))
    }
}
