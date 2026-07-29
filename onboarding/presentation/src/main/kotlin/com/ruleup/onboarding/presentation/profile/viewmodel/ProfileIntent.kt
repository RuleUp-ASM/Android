package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.domain.entity.user.InterestCategory
import com.ruleup.onboarding.domain.entity.Agreement
import com.ruleup.ui.mvi.MviIntent

sealed interface ProfileIntent : MviIntent {
    data class SetSignupToken(
        val token: String,
    ) : ProfileIntent

    data class SetNickName(
        val name: String,
    ) : ProfileIntent

    data class SetProfileIcon(
        val img: String,
    ) : ProfileIntent

    data class SetProfileInterest(
        val interestCategory: InterestCategory,
    ) : ProfileIntent

    data class SetAgreements(
        val agreements: Agreement,
    ) : ProfileIntent

    /** 가입 기본정보 — 만 나이 입력. 비우면 null. */
    data class SetAge(
        val age: Int?,
    ) : ProfileIntent

    /** 가입 기본정보 — 성별 카드 선택(같은 값 재선택 시 해제). */
    data class SetGender(
        val gender: OnboardingGender,
    ) : ProfileIntent

    /** 가입 기본정보 — "응답하지 않을래요" 토글. */
    data object DeclineGender : ProfileIntent

    /** 닉네임 페이지 "다음" — 형식·중복 검사 후 통과하면 관심사 페이지로 이동한다. */
    data object CheckNickname : ProfileIntent

    /** 약관 페이지 "시작하기" — 가입을 제출하고 성공 시 홈으로 이동한다. */
    data object Submit : ProfileIntent
}
