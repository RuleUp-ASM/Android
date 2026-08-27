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

    /** 만 14세 미만. 가입이 불가하다. */
    data object Underage : BirthDateValidation
}

/**
 * 생일 검증. **만 14세 미만은 법적으로 가입할 수 없다** — 서버도 재검증하지만 여기서 걸러야 약관까지
 * 다 채운 뒤 마지막 제출에서 `BIRTHDATE_UNDERAGE` 로 튕기지 않는다.
 *
 * 지는 규칙은 **연령 제한 하나**다. 미래 날짜는 나이 계산이 이미 걸러내고, 없는 날짜만 [LocalDate]
 * 생성에서 잡는다 — 8자리 한 필드라 화면이 막을 수 없다.
 *
 * [clock] 은 만 14세가 되는 당일 같은 경계를 테스트가 재현할 수 있어야 해서 의존으로 받는다.
 */
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
            if (Period.between(birthDate, LocalDate.now(clock)).years < MIN_AGE) {
                return BirthDateValidation.Underage
            }
            return BirthDateValidation.Valid(birthDate)
        }

        companion object {
            const val MIN_AGE = 14
        }
    }
