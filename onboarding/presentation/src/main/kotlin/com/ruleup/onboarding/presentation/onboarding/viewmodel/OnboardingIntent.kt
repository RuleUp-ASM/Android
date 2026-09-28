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

    /** 생일 입력. */
    data class SetBirthDate(
        val digits: String,
    ) : OnboardingIntent

    /** 성별 선택. */
    data class SetGender(
        val gender: Gender,
    ) : OnboardingIntent

    data class ToggleAgreement(
        val type: AgreementType,
    ) : OnboardingIntent

    /** 전체 동의 토글. */
    data object ToggleAllAgreements : OnboardingIntent

    /** 1단계 뒤로가기. */
    data object BackFromFirstStep : OnboardingIntent

    /** 약관 페이지 "시작하기" */
    data object Submit : OnboardingIntent
}
