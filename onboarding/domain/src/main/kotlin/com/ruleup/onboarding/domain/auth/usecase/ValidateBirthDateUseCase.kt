package com.ruleup.onboarding.domain.auth.usecase

import java.time.Clock
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/** 생일 입력 검증 결과. */
sealed interface BirthDateValidation {
    data class Valid(
        val birthDate: LocalDate,
    ) : BirthDateValidation

    /** 달력에 없는 날짜(2월 30일, 13월 등). */
    data object Invalid : BirthDateValidation

    /** 만 14세 미만. */
    data object Underage : BirthDateValidation
}

/** 생일 검증. */
class ValidateBirthDateUseCase
    @Inject
    constructor(
        private val clock: Clock,
    ) {
        operator fun invoke(
            year: Int,
            month: Int,
            day: Int,
        ): BirthDateValidation {
            val birthDate =
                runCatching { LocalDate.of(year, month, day) }.getOrNull()
                    ?: return BirthDateValidation.Invalid
            if (birthDate.isAfter(LocalDate.now(clock))) return BirthDateValidation.Invalid
            if (Period.between(birthDate, LocalDate.now(clock)).years < MIN_AGE) {
                return BirthDateValidation.Underage
            }
            return BirthDateValidation.Valid(birthDate)
        }

        companion object {
            const val MIN_AGE = 14
        }
    }
