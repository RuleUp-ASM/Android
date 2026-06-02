package com.ruleup.presentation.profile.vm

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.ui.mvi.MviIntent
import com.ruleup.domain.auth.model.Agreements

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
        val agreements: Agreements,
    ) : ProfileIntent

    /** "다음" — 다음 단계로. 마지막 단계에서는 제출/홈 이동을 트리거한다. */
    data object NextStep : ProfileIntent

    /** 뒤로가기 — 이전 단계로. 첫 단계에서는 무시된다. */
    data object PrevStep : ProfileIntent
}
