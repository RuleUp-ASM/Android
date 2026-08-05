package com.ruleup.onboarding.presentation.profile.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.onboarding.domain.entity.AgreementType
import com.ruleup.onboarding.domain.entity.Gender
import com.ruleup.ui.mvi.ReducerEvent
import java.time.LocalDate

sealed interface ProfileReducerEvent : ReducerEvent {
    data class SetSignupToken(
        val token: String,
    ) : ProfileReducerEvent

    data class NicknameEntered(
        val nickname: String,
    ) : ProfileReducerEvent

    data class InterestsSelected(
        val interest: Category,
    ) : ProfileReducerEvent

    data class ProfileImageSelected(
        val uri: String?,
    ) : ProfileReducerEvent

    /** 검증까지 마친 생일 입력. [birthDate] 가 null 이면 아직 유효하지 않다. */
    data class BirthDateEntered(
        val digits: String,
        val birthDate: LocalDate?,
        val error: String?,
    ) : ProfileReducerEvent

    data class GenderSelected(
        val gender: Gender,
    ) : ProfileReducerEvent

    data class AgreementToggled(
        val type: AgreementType,
    ) : ProfileReducerEvent

    data object AllAgreementsToggled : ProfileReducerEvent

    data object Submitting : ProfileReducerEvent

    data object SubmitFailed : ProfileReducerEvent
}
