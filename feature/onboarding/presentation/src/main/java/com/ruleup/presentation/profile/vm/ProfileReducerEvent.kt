package com.ruleup.presentation.profile.vm

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.ui.mvi.ReducerEvent
import com.ruleup.domain.model.Agreements

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
        val agreements: Agreements,
    ) : ProfileReducerEvent

    data object Submitting : ProfileReducerEvent

    data object SubmitFailed : ProfileReducerEvent

    data class StepChanged(
        val step: Int,
    ) : ProfileReducerEvent
}
