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

    /** 해제 확인 시트를 연다. */
    data class ConfirmUnblock(
        val target: BlockTarget,
    ) : BlockListIntent

    data object DismissConfirm : BlockListIntent

    data object Unblock : BlockListIntent
}

/** 해제 확인 중인 대상. */
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
    /** 두 갈래가 모두 비었는지 */
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

/** 일회성 이펙트 없음. */
typealias BlockListEffect = NoEffect
