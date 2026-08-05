package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.ui.mvi.MviIntent

sealed interface OnboardingIntent : MviIntent {
    data class SetNickName(
        val name: String,
    ) : OnboardingIntent

    data class SetProfileIcon(
        val img: String,
    ) : OnboardingIntent

    data class SetProfileInterest(
        val interestCategory: Category,
    ) : OnboardingIntent

    /** 생일 입력. 숫자만 8자리(YYYYMMDD)로 누적되며 검증은 8자리가 찼을 때 돈다. */
    data class SetBirthDate(
        val digits: String,
    ) : OnboardingIntent

    /** 성별 선택. 같은 값을 다시 고르면 해제된다(= 건너뛴 것과 같은 상태). */
    data class SetGender(
        val gender: Gender,
    ) : OnboardingIntent

    data class ToggleAgreement(
        val type: AgreementType,
    ) : OnboardingIntent

    /** 전체 동의 토글. 하나라도 빠져 있으면 모두 체크하고, 다 차 있으면 모두 해제한다. */
    data object ToggleAllAgreements : OnboardingIntent

    /** 1단계 뒤로가기. 지금 나가면 처음부터 다시 해야 해서 확인을 받는다. */
    data object BackFromFirstStep : OnboardingIntent

    /** 약관 페이지 "시작하기" — 가입을 제출하고 성공 시 홈으로 이동한다. */
    data object Submit : OnboardingIntent
}
