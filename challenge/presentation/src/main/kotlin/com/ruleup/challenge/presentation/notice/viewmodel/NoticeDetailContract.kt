package com.ruleup.challenge.presentation.notice.viewmodel

import com.ruleup.challenge.domain.entity.NoticeDetail
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface NoticeDetailIntent : MviIntent {
    /** 화면 진입 시 상세 조회 — 서버가 이 조회로 읽음 upsert(멱등)한다. */
    data class Load(
        val challengeId: String,
        val noticeId: String,
        val canManage: Boolean,
    ) : NoticeDetailIntent

    /** 재진입(ON_RESUME) 시 재조회 — 수정 화면에서 돌아오면 내용이 바뀐다. */
    data object Refresh : NoticeDetailIntent

    /** (방장) 고정 토글 — 단일 pin, 새 고정 시 기존 고정은 서버가 자동 해제. */
    data object TogglePin : NoticeDetailIntent

    /** (방장) 수정 화면으로 이동. */
    data object Edit : NoticeDetailIntent

    /** (방장) 삭제 확인 다이얼로그 노출/닫기. */
    data class SetDeleteDialog(
        val visible: Boolean,
    ) : NoticeDetailIntent

    /** (방장) 삭제 확정 — 성공 시 목록으로 복귀. */
    data object ConfirmDelete : NoticeDetailIntent

    data object Back : NoticeDetailIntent
}

sealed interface NoticeDetailEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : NoticeDetailEffect
}

data class NoticeDetailState(
    val challengeId: String,
    val noticeId: String,
    val canManage: Boolean,
    val isLoading: Boolean,
    val detail: NoticeDetail?,
    val errorMessage: String?,
    // 삭제 확인 다이얼로그 노출 여부
    val showDeleteDialog: Boolean = false,
    // 고정/삭제 요청 중 (중복 탭 방지)
    val isMutating: Boolean = false,
) : UiState {
    companion object {
        val initial =
            NoticeDetailState(
                challengeId = "",
                noticeId = "",
                canManage = false,
                isLoading = true,
                detail = null,
                errorMessage = null,
            )
    }
}

sealed interface NoticeDetailReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
        val noticeId: String,
        val canManage: Boolean,
    ) : NoticeDetailReducerEvent

    data class Loaded(
        val detail: NoticeDetail,
    ) : NoticeDetailReducerEvent

    data class Failed(
        val message: String,
    ) : NoticeDetailReducerEvent

    data class DeleteDialog(
        val visible: Boolean,
    ) : NoticeDetailReducerEvent

    data class Mutating(
        val mutating: Boolean,
    ) : NoticeDetailReducerEvent

    /** 고정 토글 성공 — 서버 최종 상태로 반영. */
    data class PinChanged(
        val pinned: Boolean,
    ) : NoticeDetailReducerEvent
}
