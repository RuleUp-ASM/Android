package com.ruleup.onboarding.presentation.onboarding.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.domain.entity.user.Gender
import com.ruleup.ui.mvi.ReducerEvent
import java.time.LocalDate

sealed interface OnboardingReducerEvent : ReducerEvent {
    data class NicknameEntered(
        val nickname: String,
    ) : OnboardingReducerEvent

    /** 디바운스된 확인 결과. [available] 이 null 이면 확인 전 상태로 되돌린다. */
    data class NicknameChecked(
        val available: Boolean?,
        val message: String?,
    ) : OnboardingReducerEvent

    data class InterestsSelected(
        val interest: Category,
    ) : OnboardingReducerEvent

    data class ProfileImageSelected(
        val uri: String?,
    ) : OnboardingReducerEvent

    /** 검증까지 마친 생일 입력. [birthDate] 가 null 이면 아직 유효하지 않다. */
    data class BirthDateEntered(
        val digits: String,
        val birthDate: LocalDate?,
        val error: String?,
    ) : OnboardingReducerEvent

    data class GenderSelected(
        val gender: Gender,
    ) : OnboardingReducerEvent

    data class AgreementToggled(
        val type: AgreementType,
    ) : OnboardingReducerEvent

    data object AllAgreementsToggled : OnboardingReducerEvent

    data object Submitting : OnboardingReducerEvent

    data object SubmitFailed : OnboardingReducerEvent
}
