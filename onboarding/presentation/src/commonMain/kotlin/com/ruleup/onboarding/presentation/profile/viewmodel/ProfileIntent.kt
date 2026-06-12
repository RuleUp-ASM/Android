package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.InterestCategory
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

    /** 닉네임 페이지 "다음" — 형식·중복 검사 후 통과하면 관심사 페이지로 이동한다. */
    data object CheckNickname : ProfileIntent

    /** 약관 페이지 "시작하기" — 가입을 제출하고 성공 시 홈으로 이동한다. */
    data object Submit : ProfileIntent
}
