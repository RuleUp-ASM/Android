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
 * 생일 검증. **만 14세 미만은 법적으로 가입할 수 없다.**
 *
 * 서버가 재검증하지만 클라도 먼저 본다 — 여기서 걸러야 사용자가 약관까지 다 채운 뒤 마지막
 * 제출에서 `BIRTHDATE_UNDERAGE` 로 튕기지 않는다.
 *
 * 지는 규칙은 **연령 제한 하나**다. 미래 날짜를 따로 막지 않는 건 나이 계산이 이미 걸러내기
 * 때문이고, 애초에 미래를 못 넣게 하는 건 입력 화면의 몫이다. 없는 날짜만 [LocalDate] 생성 단계에서
 * 걸러낸다 — 8자리 한 필드라 화면이 막을 수 없다.
 *
 * [clock] 을 주입받는 이유는 "오늘"이 고정되지 않으면 만 14세가 되는 당일 같은 경계를 재현할 수
 * 없어서다. 테스트가 시그니처를 왜곡하지 않도록 인자가 아니라 의존으로 받는다.
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
