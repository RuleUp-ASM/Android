package com.ruleup.challenge.presentation.notice.viewmodel

import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface NoticeListIntent : MviIntent {
    /** 화면 진입 시 목록 조회. [canManage] 는 방 홈이 판정한 방장 여부(작성 버튼 노출용). */
    data class Load(
        val challengeId: String,
        val canManage: Boolean,
    ) : NoticeListIntent

    /** 재진입(ON_RESUME) 시 재조회 — 상세를 읽고 오면 isRead, 작성/삭제 후엔 목록이 바뀐다. */
    data object Refresh : NoticeListIntent

    /** 항목 탭 → 공지 상세(= 읽음 처리). */
    data class OpenNotice(
        val noticeId: String,
    ) : NoticeListIntent

    /** (방장) 작성 화면으로 이동. */
    data object CreateNotice : NoticeListIntent

    data object Back : NoticeListIntent
}

data class NoticeListState(
    val challengeId: String,
    val canManage: Boolean,
    val isLoading: Boolean,
    val notices: List<NoticeSummary>,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            NoticeListState(
                challengeId = "",
                canManage = false,
                isLoading = true,
                notices = emptyList(),
                errorMessage = null,
            )
    }
}

sealed interface NoticeListReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
        val canManage: Boolean,
    ) : NoticeListReducerEvent

    data class Loaded(
        val notices: List<NoticeSummary>,
    ) : NoticeListReducerEvent

    data class Failed(
        val message: String,
    ) : NoticeListReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias NoticeListEffect = NoEffect
