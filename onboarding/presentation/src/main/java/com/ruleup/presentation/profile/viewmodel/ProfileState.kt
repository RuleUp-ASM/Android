package com.ruleup.presentation.profile.viewmodel

import com.ruleup.entity.user.Agreement
import com.ruleup.entity.user.InterestCategory
import com.ruleup.ui.mvi.UiState

/**
 * 프로필 설정 플로우의 누적 상태. 페이지별 화면(아이콘→닉네임→관심사→권한→약관)이
 * 같은 ViewModel 을 공유하므로, step 없이 입력값만 누적한다.
 */
data class ProfileState(
    val signupToken: String? = null,
    val nickname: String = "",
    val interests: List<InterestCategory> = emptyList(),
    val profileImageUrl: String? = null,
    val agreements: Agreement = Agreement(terms = false, privacy = false, marketing = false),
    val isSubmitting: Boolean = false,
) : UiState {
    companion object {
        val initial = ProfileState()
    }
}
