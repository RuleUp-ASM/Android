package com.ruleup.challenge.presentation.notice.viewmodel

import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface NoticeEditIntent : MviIntent {
    /** 진입 — [noticeId] 가 null 이면 작성, 있으면 기존 공지를 조회해 수정 모드로 채운다. */
    data class Load(
        val challengeId: String,
        val noticeId: String?,
    ) : NoticeEditIntent

    data class ChangeTitle(
        val title: String,
    ) : NoticeEditIntent

    data class ChangeContent(
        val content: String,
    ) : NoticeEditIntent

    /** (작성 모드) 고정으로 등록 토글 — 기존 고정은 서버가 자동 해제(단일 pin). */
    data class TogglePinned(
        val pinned: Boolean,
    ) : NoticeEditIntent

    /** (수정 모드) 읽음 초기화 토글 — 규칙 변경 등 재확인 필요 시(재발송 포함). */
    data class ToggleResetRead(
        val resetRead: Boolean,
    ) : NoticeEditIntent

    /** 저장 (작성 = POST · 수정 = PUT). 성공 시 이전 화면으로 복귀. */
    data object Save : NoticeEditIntent

    data object Back : NoticeEditIntent
}

sealed interface NoticeEditEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : NoticeEditEffect
}

data class NoticeEditState(
    val challengeId: String,
    // null = 작성 모드
    val noticeId: String?,
    // 수정 모드에서 기존 공지 조회 중
    val isLoading: Boolean,
    val title: String,
    val content: String,
    // 작성 모드 전용
    val pinned: Boolean,
    // 수정 모드 전용
    val resetRead: Boolean,
    val isSaving: Boolean,
    val errorMessage: String?,
) : UiState {
    val isEditMode: Boolean get() = noticeId != null

    companion object {
        val initial =
            NoticeEditState(
                challengeId = "",
                noticeId = null,
                isLoading = false,
                title = "",
                content = "",
                pinned = false,
                resetRead = false,
                isSaving = false,
                errorMessage = null,
            )
    }
}

sealed interface NoticeEditReducerEvent : ReducerEvent {
    data class Init(
        val challengeId: String,
        val noticeId: String?,
    ) : NoticeEditReducerEvent

    /** 수정 모드 — 기존 공지 내용으로 폼을 채운다. */
    data class Prefilled(
        val title: String,
        val content: String,
    ) : NoticeEditReducerEvent

    data class PrefillFailed(
        val message: String,
    ) : NoticeEditReducerEvent

    data class TitleChanged(
        val title: String,
    ) : NoticeEditReducerEvent

    data class ContentChanged(
        val content: String,
    ) : NoticeEditReducerEvent

    data class PinnedChanged(
        val pinned: Boolean,
    ) : NoticeEditReducerEvent

    data class ResetReadChanged(
        val resetRead: Boolean,
    ) : NoticeEditReducerEvent

    data class Saving(
        val saving: Boolean,
    ) : NoticeEditReducerEvent
}
