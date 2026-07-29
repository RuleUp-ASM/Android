package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.domain.entity.user.InterestCategory
import com.ruleup.onboarding.domain.entity.Agreement
import com.ruleup.ui.mvi.ReducerEvent

sealed interface ProfileReducerEvent : ReducerEvent {
    data class SetSignupToken(
        val token: String,
    ) : ProfileReducerEvent

    data class NicknameEntered(
        val nickname: String,
    ) : ProfileReducerEvent

    data class InterestsSelected(
        val interest: InterestCategory,
    ) : ProfileReducerEvent

    data class ProfileImageSelected(
        val url: String?,
    ) : ProfileReducerEvent

    data class AgreementsUpdated(
        val agreements: Agreement,
    ) : ProfileReducerEvent

    data class AgeEntered(
        val age: Int?,
    ) : ProfileReducerEvent

    data class GenderSelected(
        val gender: OnboardingGender,
    ) : ProfileReducerEvent

    data object GenderDeclined : ProfileReducerEvent

    data object Submitting : ProfileReducerEvent

    data object SubmitFailed : ProfileReducerEvent
}
