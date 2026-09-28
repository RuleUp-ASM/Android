package com.ruleup.verification.presentation.manual.viewmodel

import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.TodayResultStatus

sealed interface ManualSubmitIntent : MviIntent {
    data class Load(
        val challengeId: String,
    ) : ManualSubmitIntent

    data object Retry : ManualSubmitIntent

    data object Back : ManualSubmitIntent

    data class NoteChanged(
        val value: String,
    ) : ManualSubmitIntent

    data object Submit : ManualSubmitIntent

    /** 체크 해제. */
    data object Uncheck : ManualSubmitIntent
}

/** 수동 인증 화면 상태. */
data class ManualSubmitState(
    val challengeId: String,
    val isLoading: Boolean,
    val title: String,
    // 오늘 (KST, `2026-09-14`).
    val date: String,
    // "자정 마감" 같은 인증 창 문구.
    val window: String?,
    val status: TodayResultStatus?,
    // 체크 해제의 키.
    val verificationId: String?,
    val streakAfter: Int?,
    val note: String,
    val isSubmitting: Boolean,
    val errorMessage: String?,
) : UiState {
    /** 오늘 체크가 끝났는가. */
    val checked: Boolean
        get() = status == TodayResultStatus.DONE

    /** 오늘이 이 챌린지의 대상일이 아니면 체크할 것이 없다. */
    val notTarget: Boolean
        get() = status == TodayResultStatus.NOT_TARGET

    val canSubmit: Boolean
        get() = !isLoading && !isSubmitting && !checked && !notTarget

    val canUncheck: Boolean
        get() = !isSubmitting && checked && verificationId != null

    /** 오늘 상태를 받지 못했다 */
    val loadFailed: Boolean
        get() = !isLoading && status == null && errorMessage != null

    companion object {
        fun initial(challengeId: String) =
            ManualSubmitState(
                challengeId = challengeId,
                isLoading = true,
                title = "",
                date = "",
                window = null,
                status = null,
                verificationId = null,
                streakAfter = null,
                note = "",
                isSubmitting = false,
                errorMessage = null,
            )
    }
}

sealed interface ManualSubmitReducerEvent : ReducerEvent {
    /** 챌린지 식별자를 포함한 오늘 인증 결과. */
    data class Loading(
        val challengeId: String,
        val silent: Boolean,
    ) : ManualSubmitReducerEvent

    data class Loaded(
        val title: String,
        val date: String,
        val window: String?,
        val status: TodayResultStatus?,
        val verificationId: String?,
        val streakAfter: Int?,
        val keepMessage: Boolean,
    ) : ManualSubmitReducerEvent

    data class Failed(
        val message: String,
    ) : ManualSubmitReducerEvent

    data class NoteEdited(
        val value: String,
    ) : ManualSubmitReducerEvent

    data class Submitting(
        val submitting: Boolean,
    ) : ManualSubmitReducerEvent

    data class Submitted(
        val verificationId: String,
        val status: TodayResultStatus?,
        val streakAfter: Int?,
    ) : ManualSubmitReducerEvent

    data object Unchecked : ManualSubmitReducerEvent
}

/** 일회성 이펙트 없음. */
typealias ManualSubmitEffect = NoEffect
