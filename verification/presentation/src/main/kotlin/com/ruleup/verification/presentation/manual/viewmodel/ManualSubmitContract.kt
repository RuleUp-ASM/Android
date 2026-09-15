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

    /** 체크 해제. 당일 안에서만 되고, 기한이 지나면 서버가 막는다. */
    data object Uncheck : ManualSubmitIntent
}

/**
 * 수동 인증 화면 상태 (Figma `1443:2`).
 *
 * [title] 은 진행률 조회에서 온다 — 실패해도 비운 채로 화면을 세운다. 제목을 못 받았다고 체크를
 * 막으면 **사용자가 오늘 인증을 못 하게 된다.** 체크에 필요한 것은 challengeId 하나뿐이다.
 */
data class ManualSubmitState(
    val challengeId: String,
    val isLoading: Boolean,
    val title: String,
    // 오늘 (KST, `2026-09-14`). 기기 시계가 아니라 서버가 정한 날을 쓴다.
    val date: String,
    // "자정 마감" 같은 인증 창 문구. 서버가 주지 않으면 표기를 생략한다.
    val window: String?,
    val status: TodayResultStatus?,
    // 체크 해제의 키. 없으면 되돌릴 경로가 없어 해제 버튼을 두지 않는다.
    val verificationId: String?,
    val streakAfter: Int?,
    val note: String,
    val isSubmitting: Boolean,
    val errorMessage: String?,
) : UiState {
    /** 오늘 체크가 끝났는가. 확정 상태만 본다 — 실패 예정·진행중은 아직 체크 전이다. */
    val checked: Boolean
        get() = status == TodayResultStatus.DONE

    /** 오늘이 이 챌린지의 대상일이 아니면 체크할 것이 없다. */
    val notTarget: Boolean
        get() = status == TodayResultStatus.NOT_TARGET

    val canSubmit: Boolean
        get() = !isLoading && !isSubmitting && !checked && !notTarget

    val canUncheck: Boolean
        get() = !isSubmitting && checked && verificationId != null

    /** 오늘 상태를 받지 못했다 — 카드 대신 재시도를 보여 준다. */
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
    /**
     * challengeId 를 함께 싣는다 — 재시도가 같은 챌린지를 다시 부르려면 상태가 기억해야 한다.
     *
     * [silent] 는 제출·해제가 실패해 **서버 상태만 다시 맞추는** 새로고침이다. 스피너를 띄우지 않고
     * 직전 오류 문구를 지우지 않는다 — 지우면 "이미 체크했어요"를 읽기도 전에 사라진다.
     */
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

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias ManualSubmitEffect = NoEffect
