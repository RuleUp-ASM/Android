package com.ruleup.presentation.profile.vm

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.ui.mvi.MviIntent
import java.security.Permissions

sealed interface ProfileIntent : MviIntent {

    data class SetNickName(
        val name: String,
    ) : ProfileIntent

    data class SetProfileIcon(
        val img: String,
    ) : ProfileIntent

    data class SetProfileInterest(
        val interestCategories: List<InterestCategory>,
    ) : ProfileIntent

    data class SetPermission(
        val permissions: Permissions,
    ) : ProfileIntent
}
