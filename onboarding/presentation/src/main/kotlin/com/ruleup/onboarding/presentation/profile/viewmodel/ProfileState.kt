package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.domain.entity.user.InterestCategory
import com.ruleup.onboarding.domain.entity.Agreement
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
    // 가입 기본정보(선택) — 만 나이, 성별. 미입력/건너뛰기면 null.
    val age: Int? = null,
    val gender: OnboardingGender? = null,
    // "응답하지 않을래요" 명시적 선택. gender 는 null 이지만 미선택과 구분해 UI 라디오를 채운다.
    val genderDeclined: Boolean = false,
    val isSubmitting: Boolean = false,
) : UiState {
    companion object {
        val initial = ProfileState()
    }
}
