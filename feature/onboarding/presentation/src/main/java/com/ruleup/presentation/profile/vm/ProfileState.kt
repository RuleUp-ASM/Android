package com.ruleup.presentation.profile.vm

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.ui.mvi.UiState
import com.ruleup.domain.model.Agreements

data class ProfileState(
    val signupToken: String? = null,
    val nickname: String? = null,
    val interests: List<InterestCategory> = emptyList(),
    val profileImageUrl: String? = null,
    val agreements: Agreements? = null,
    val isSubmitting: Boolean = false,
) : UiState {
    companion object {
        val initial = ProfileState()
    }
}
