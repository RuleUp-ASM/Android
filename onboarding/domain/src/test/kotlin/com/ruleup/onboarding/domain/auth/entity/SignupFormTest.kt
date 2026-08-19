package com.ruleup.onboarding.domain.auth.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.category.InterestLimits
import com.ruleup.domain.entity.user.AgreementConsents
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.domain.entity.user.TermsVersions
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SignupFormTest {
    @Test
    fun `관심 분야는 상한까지만 담을 수 있다`() {
        assertFailsWith<IllegalArgumentException> {
            form(Category.entries.take(InterestLimits.MAX + 1))
        }
    }

    @Test
    fun `관심 분야는 하한이 없어 건너뛸 수 있다`() {
        // 화면이 "아무것도 안 고르고 넘어가기"를 허용하므로 빈 리스트가 정상 입력이다.
        assertEquals(emptyList(), form(emptyList()).interestCategories)
    }
}

private fun form(interests: List<Category>) =
    SignupForm(
        signupToken = "signup-token",
        nickname = "nick",
        interestCategories = interests,
        birthDate = LocalDate.of(2000, 5, 27),
        gender = Gender.NON_BINARY,
        agreements = AgreementConsents.of(AgreementType.REQUIRED.toSet(), TermsVersions(emptyMap())),
    )
