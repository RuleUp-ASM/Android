package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.ui.mvi.UiState
import java.time.LocalDate

/** 프로필 설정 플로우의 누적 상태. */
data class OnboardingState(
    val nickname: String = "",
    // 실시간 확인 결과.
    val nicknameAvailable: Boolean? = null,
    val nicknameMessage: String? = null,
    val interests: List<Category> = emptyList(),
    val profileImageUri: String? = null,
    val birthDate: LocalDate? = null,
    val birthDateInput: String = "",
    val birthDateError: String? = null,
    val gender: Gender? = null,
    val agreements: Set<AgreementType> = emptySet(),
    val isSubmitting: Boolean = false,
) : UiState {
    /** 서버 확인까지 통과해야 다음 단계로 보낸다. */
    val nicknameConfirmed: Boolean get() = nicknameAvailable == true

    val requiredAgreementsSatisfied: Boolean
        get() = AgreementType.REQUIRED.all { it in agreements }

    companion object {
        val initial = OnboardingState()

        /** 생년월일 입력 자릿수(YYYYMMDD). */
        const val BIRTH_DATE_LENGTH = 8
    }
}
