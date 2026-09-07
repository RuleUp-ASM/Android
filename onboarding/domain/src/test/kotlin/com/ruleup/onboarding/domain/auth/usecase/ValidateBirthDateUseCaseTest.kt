package com.ruleup.onboarding.domain.auth.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ValidateBirthDateUseCaseTest {
    // 만 14세가 되는 당일이 경계라, "오늘"을 고정하지 않으면 재현할 수 없다.
    private val useCase = ValidateBirthDateUseCase(clockAt("2026-08-05"))

    @Test
    fun `만 14세 이상이면 통과한다`() {
        assertEquals(BirthDateValidation.Valid(LocalDate.of(2000, 5, 27)), useCase(2000, 5, 27))
    }

    @Test
    fun `만 14세가 되는 당일은 통과한다`() {
        // 하루라도 밀리면 생일 당일에 가입이 막힌다.
        assertEquals(BirthDateValidation.Valid(LocalDate.of(2012, 8, 5)), useCase(2012, 8, 5))
    }

    @Test
    fun `만 14세 생일 하루 전은 막는다`() {
        assertEquals(BirthDateValidation.Underage, useCase(2012, 8, 6))
    }

    @Test
    fun `달력에 없는 날짜는 형식 오류로 본다`() {
        // 8자리 한 필드라 화면이 막을 수 없다.
        assertEquals(BirthDateValidation.Invalid, useCase(2000, 2, 30))
        assertEquals(BirthDateValidation.Invalid, useCase(2000, 13, 1))
    }

    @Test
    fun `미래 날짜는 연령 제한에서 걸린다`() {
        // 따로 검사하지 않는다 — 나이 계산이 이미 걸러내고, 입력을 막는 건 화면의 몫이다.
        assertEquals(BirthDateValidation.Underage, useCase(2027, 1, 1))
    }

    private fun clockAt(date: String): Clock = Clock.fixed(LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC)
}
