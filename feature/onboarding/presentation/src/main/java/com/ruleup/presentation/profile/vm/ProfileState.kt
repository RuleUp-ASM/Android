package com.ruleup.presentation.profile.vm

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.ui.mvi.UiState
import com.ruleup.domain.auth.model.Agreements

data class ProfileState(
    val step: Int = 0,
    val signupToken: String? = null,
    val nickname: String = "",
    val interests: List<InterestCategory> = emptyList(),
    val profileImageUrl: String? = null,
    val agreements: Agreements = Agreements(terms = false, privacy = false, marketing = false),
    val isSubmitting: Boolean = false,
) : UiState {
    companion object {
        /** 프로필 설정 마지막 단계 인덱스 (0:아이콘, 1:닉네임, 2:관심사, 3:권한, 4:약관동의). */
        const val LAST_STEP = 4
        val initial = ProfileState()
    }
}
