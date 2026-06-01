package com.ruleup.presentation.profile.vm

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.ui.mvi.ReducerEvent
import com.ruleup.domain.model.Agreements

sealed interface ProfileReducerEvent : ReducerEvent {
    data class NicknameEntered(
        val nickname: String,
    ) : ProfileReducerEvent

    data class InterestsSelected(
        val interests: List<InterestCategory>,
    ) : ProfileReducerEvent

    data class ProfileImageSelected(
        val url: String?,
    ) : ProfileReducerEvent

    data class AgreementsUpdated(
        val agreements: Agreements,
    ) : ProfileReducerEvent

    data object Submitting : ProfileReducerEvent
}
