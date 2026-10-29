package com.ruleup.onboarding.presentation.common

import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewLoginFailureUiTest {
    @Test
    fun `심사 코드 오류의 종류에 따라 적절한 안내를 보여준다`() {
        val cases =
            mapOf(
                AuthFailure.INVALID_REVIEW_CODE to "코드를 다시 확인해주세요",
                AuthFailure.REVIEW_TOO_MANY_ATTEMPTS to "잠시 후 다시 시도해주세요",
                AuthFailure.REVIEW_UNAVAILABLE to "현재 심사 로그인을 사용할 수 없어요",
            )

        cases.forEach { (failure, message) ->
            assertEquals(AuthFailureUi.Toast(message), AuthException(failure).toAuthFailureUi())
        }
    }
}
