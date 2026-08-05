package com.ruleup.onboarding.domain.auth.usecase

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateBirthDateUseCaseTest {
    private val useCase = ValidateBirthDateUseCase()
    private val today = LocalDate.of(2026, 8, 5)

    @Test
    fun `만 14세 이상이면 통과한다`() {
        val result = useCase(2000, 5, 27, today)

        assertEquals(BirthDateValidation.Valid(LocalDate.of(2000, 5, 27)), result)
    }

    @Test
    fun `만 14세가 되는 당일은 통과한다`() {
        // 경계. 하루라도 밀리면 생일 당일에 가입이 막힌다.
        val result = useCase(2012, 8, 5, today)

        assertEquals(BirthDateValidation.Valid(LocalDate.of(2012, 8, 5)), result)
    }

    @Test
    fun `만 14세 생일 하루 전은 막는다`() {
        val result = useCase(2012, 8, 6, today)

        assertEquals(BirthDateValidation.Underage, result)
    }

    @Test
    fun `없는 날짜는 형식 오류로 본다`() {
        assertEquals(BirthDateValidation.Invalid, useCase(2000, 2, 30, today))
        assertEquals(BirthDateValidation.Invalid, useCase(2000, 13, 1, today))
    }

    @Test
    fun `미래 날짜는 형식 오류로 본다`() {
        assertEquals(BirthDateValidation.Invalid, useCase(2027, 1, 1, today))
    }
}
