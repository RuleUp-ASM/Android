package com.ruleup.report.presentation.blocklist.viewmodel

import com.ruleup.report.domain.entity.BlockList
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface BlockListIntent : MviIntent {
    data object Load : BlockListIntent

    data object Retry : BlockListIntent

    data object Back : BlockListIntent

    /** 해제 확인 시트를 연다. 바로 풀지 않는 이유는 [BlockTarget] 의 KDoc 참고. */
    data class ConfirmUnblock(
        val target: BlockTarget,
    ) : BlockListIntent

    data object DismissConfirm : BlockListIntent

    data object Unblock : BlockListIntent
}

/**
 * 해제 확인 중인 대상.
 *
 * 확인 없이 바로 풀지 않는다 — 해제는 되돌리려면 그 사람을 **다시 신고**해야 하고, 그러면
 * 신고 건이 하나 더 쌓인다. 실수로 눌러 원치 않는 신고를 만들게 두지 않는다.
 */
sealed interface BlockTarget {
    val id: String
    val label: String

    data class User(
        override val id: String,
        override val label: String,
    ) : BlockTarget

    data class Challenge(
        override val id: String,
        override val label: String,
    ) : BlockTarget
}

data class BlockListState(
    val isLoading: Boolean,
    val blocks: BlockList,
    val errorMessage: String?,
    // null 이면 확인 시트가 닫힌 상태다.
    val confirming: BlockTarget?,
    val unblocking: Boolean,
) : UiState {
    /** 두 갈래가 모두 비었는지 — 빈 상태 문구를 가르는 기준(Figma `1287:2`). */
    val isEmpty: Boolean
        get() = blocks.isEmpty

    companion object {
        val initial =
            BlockListState(
                isLoading = true,
                blocks = BlockList(users = emptyList(), challenges = emptyList()),
                errorMessage = null,
                confirming = null,
                unblocking = false,
            )
    }
}

sealed interface BlockListReducerEvent : ReducerEvent {
    data object Loading : BlockListReducerEvent

    data class Loaded(
        val blocks: BlockList,
    ) : BlockListReducerEvent

    data class Failed(
        val message: String,
    ) : BlockListReducerEvent

    data class ConfirmRequested(
        val target: BlockTarget,
    ) : BlockListReducerEvent

    data object ConfirmDismissed : BlockListReducerEvent

    data object Unblocking : BlockListReducerEvent

    data class UnblockFailed(
        val message: String,
    ) : BlockListReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias BlockListEffect = NoEffect
