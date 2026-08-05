package com.ruleup.onboarding.domain.auth.usecase

import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/** 생일 입력 검증 결과. */
sealed interface BirthDateValidation {
    data class Valid(
        val birthDate: LocalDate,
    ) : BirthDateValidation

    /** 형식이 아니거나 없는 날짜(2월 30일 등)이거나 미래다. */
    data object Invalid : BirthDateValidation

    /** 만 14세 미만. 가입이 불가하다. */
    data object Underage : BirthDateValidation
}

/**
 * 생일 검증. **만 14세 미만은 법적으로 가입할 수 없다.**
 *
 * 서버가 재검증하지만 클라도 먼저 본다 — 여기서 걸러야 사용자가 약관까지 다 채운 뒤 마지막
 * 제출에서 `BIRTHDATE_UNDERAGE` 로 튕기지 않는다.
 *
 * [today] 를 인자로 받는 건 테스트 때문이다. "오늘"이 고정되지 않으면 만 14세가 되는 당일 같은
 * 경계를 재현할 수 없다.
 */
class ValidateBirthDateUseCase
    @Inject
    constructor() {
        operator fun invoke(
            year: Int,
            month: Int,
            day: Int,
            today: LocalDate = LocalDate.now(),
        ): BirthDateValidation {
            val birthDate = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return BirthDateValidation.Invalid
            if (birthDate.isAfter(today)) return BirthDateValidation.Invalid
            if (Period.between(birthDate, today).years < MIN_AGE) return BirthDateValidation.Underage
            return BirthDateValidation.Valid(birthDate)
        }

        companion object {
            const val MIN_AGE = 14
        }
    }
