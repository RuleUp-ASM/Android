package com.ruleup.profile.presentation.agreements.viewmodel

import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.profile.domain.entity.AgreementStatus
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface AgreementsIntent : MviIntent {
    data object Load : AgreementsIntent

    /** 선택 항목 토글. 필수 3종은 화면이 아예 누를 수 없게 둔다. */
    data class Toggle(
        val type: AgreementType,
        val agreed: Boolean,
    ) : AgreementsIntent

    /** 재동의 — `reconsentRequired` 전부를 현행 버전으로 다시 보낸다. */
    data object Reconsent : AgreementsIntent

    data object Back : AgreementsIntent
}

sealed interface AgreementsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : AgreementsEffect
}

data class AgreementsState(
    val isLoading: Boolean,
    val status: AgreementStatus?,
    // 전송 중인 항목. 응답이 올 때까지 그 행만 잠근다
    val submitting: AgreementType?,
    val isReconsenting: Boolean,
    val errorMessage: String?,
) : UiState {
    val reconsentRequired: List<AgreementType>
        get() = status?.reconsentRequired.orEmpty()

    companion object {
        val initial =
            AgreementsState(
                isLoading = true,
                status = null,
                submitting = null,
                isReconsenting = false,
                errorMessage = null,
            )
    }
}

sealed interface AgreementsReducerEvent : ReducerEvent {
    data object Loading : AgreementsReducerEvent

    data class Loaded(
        val status: AgreementStatus,
    ) : AgreementsReducerEvent

    data class Failed(
        val message: String,
    ) : AgreementsReducerEvent

    data class Submitting(
        val type: AgreementType?,
    ) : AgreementsReducerEvent

    data class Reconsenting(
        val reconsenting: Boolean,
    ) : AgreementsReducerEvent
}
