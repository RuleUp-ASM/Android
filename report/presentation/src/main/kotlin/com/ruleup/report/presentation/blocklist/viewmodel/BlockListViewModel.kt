package com.ruleup.report.presentation.blocklist.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.report.domain.entity.ReportException
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.repository.ReportRepository
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 신고한 사용자·챌린지 ViewModel (명세 GET/DELETE /users/me/blocks).
 *
 * 해제에 성공하면 목록을 **다시 불러온다.** 로컬에서 그 행만 지우면 다른 기기에서 생긴 변화가
 * 반영되지 않고, 이 화면은 그 목록이 전부라 어긋난 걸 알아챌 방법이 없다.
 */
@HiltViewModel
class BlockListViewModel
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<BlockListIntent, BlockListState, BlockListReducerEvent, NoEffect>(
            BlockListState.initial,
        ) {
        override fun onIntent(intent: BlockListIntent) {
            when (intent) {
                BlockListIntent.Load -> load(force = false)
                BlockListIntent.Retry -> load(force = true)
                BlockListIntent.Back -> navigationHelper.navigateToBack()
                is BlockListIntent.ConfirmUnblock -> dispatch(BlockListReducerEvent.ConfirmRequested(intent.target))
                BlockListIntent.DismissConfirm -> dispatch(BlockListReducerEvent.ConfirmDismissed)
                BlockListIntent.Unblock -> unblock()
            }
        }

        override fun reduce(
            state: BlockListState,
            event: BlockListReducerEvent,
        ): BlockListState =
            when (event) {
                BlockListReducerEvent.Loading -> state.copy(isLoading = true, errorMessage = null)

                is BlockListReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        blocks = event.blocks,
                        errorMessage = null,
                        // 목록을 새로 받으면 확인 시트는 닫는다 — 방금 푼 대상이 남아 있으면 안 된다.
                        confirming = null,
                        unblocking = false,
                    )

                is BlockListReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message, unblocking = false)

                is BlockListReducerEvent.ConfirmRequested -> state.copy(confirming = event.target)

                BlockListReducerEvent.ConfirmDismissed -> state.copy(confirming = null)

                BlockListReducerEvent.Unblocking -> state.copy(unblocking = true, errorMessage = null)

                is BlockListReducerEvent.UnblockFailed ->
                    state.copy(unblocking = false, confirming = null, errorMessage = event.message)
            }

        private fun load(force: Boolean) {
            if (!force && !currentState.isEmpty) return
            dispatch(BlockListReducerEvent.Loading)
            viewModelScope.launch {
                runCatching { reportRepository.getBlocks() }
                    .onSuccess { dispatch(BlockListReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(BlockListReducerEvent.Failed(it.userMessage("목록을 불러오지 못했어요"))) }
            }
        }

        private fun unblock() {
            val target = currentState.confirming ?: return
            // 두 번 눌러 같은 대상을 두 번 풀면 두 번째는 404 가 되어 오류로 보인다.
            if (currentState.unblocking) return
            dispatch(BlockListReducerEvent.Unblocking)
            viewModelScope.launch {
                runCatching {
                    when (target) {
                        is BlockTarget.User -> reportRepository.unblockUser(target.id)
                        is BlockTarget.Challenge -> reportRepository.unblockChallenge(target.id)
                    }
                }.onSuccess {
                    // 해제분이 빠진 목록을 서버에서 다시 받는다.
                    load(force = true)
                }.onFailure {
                    dispatch(BlockListReducerEvent.UnblockFailed(it.userMessage("차단을 풀지 못했어요")))
                }
            }
        }
    }

/**
 * 실패를 사용자 문구로 옮긴다.
 *
 * 이미 풀린 차단은 실패가 아니라 **경합**이다 — 다른 기기에서 먼저 풀었을 뿐이고, 사용자가 원한
 * 결과는 이미 이뤄져 있다. 그래서 오류처럼 말하지 않고 목록이 옛것이었다고만 알린다.
 */
private fun Throwable.userMessage(fallback: String): String =
    when ((this as? ReportException)?.failure) {
        ReportFailure.BLOCK_ENTRY_NOT_FOUND -> "이미 풀린 차단이에요. 목록을 새로 불러올게요."
        ReportFailure.NETWORK -> "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요."
        ReportFailure.ACCOUNT_LOCKED -> "지금은 둘러보기만 할 수 있어요."
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
