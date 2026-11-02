package com.ruleup.onboarding.presentation.common

import com.ruleup.onboarding.domain.auth.entity.AuthException
import com.ruleup.onboarding.domain.auth.entity.AuthFailure
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewLoginFailureUiTest {
    @Test
    fun `심사 종료는 코드 오류와 같은 안내로 보여 종료 여부를 드러내지 않는다`() {
        val cases =
            mapOf(
                AuthFailure.INVALID_REVIEW_CODE to "코드를 다시 확인해주세요",
                AuthFailure.REVIEW_TOO_MANY_ATTEMPTS to "잠시 후 다시 시도해주세요",
                AuthFailure.REVIEW_UNAVAILABLE to "코드를 다시 확인해주세요",
            )

        cases.forEach { (failure, message) ->
            assertEquals(AuthFailureUi.Toast(message), AuthException(failure).toAuthFailureUi())
        }
    }
}
