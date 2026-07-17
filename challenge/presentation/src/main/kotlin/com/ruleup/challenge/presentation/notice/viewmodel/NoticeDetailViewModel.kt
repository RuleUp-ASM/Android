package com.ruleup.challenge.presentation.notice.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeNoticeEditPage
import com.ruleup.challenge.domain.usecase.DeleteNoticeUseCase
import com.ruleup.challenge.domain.usecase.GetNoticeDetailUseCase
import com.ruleup.challenge.domain.usecase.PinNoticeUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 공지 상세 ViewModel. 상세 조회가 곧 읽음 처리(서버 upsert)라 별도 읽음 호출이 없다.
 * 고정/수정/삭제 메뉴는 canManage(방 홈 myRole) 기준 노출 — 실제 권한 판정은 서버(403).
 */
@HiltViewModel
class NoticeDetailViewModel
    @Inject
    constructor(
        private val getNoticeDetailUseCase: GetNoticeDetailUseCase,
        private val pinNoticeUseCase: PinNoticeUseCase,
        private val deleteNoticeUseCase: DeleteNoticeUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<NoticeDetailIntent, NoticeDetailState, NoticeDetailReducerEvent, NoticeDetailEffect>(
            NoticeDetailState.initial,
        ) {
        override fun onIntent(intent: NoticeDetailIntent) {
            when (intent) {
                is NoticeDetailIntent.Load -> load(intent.challengeId, intent.noticeId, intent.canManage)
                NoticeDetailIntent.Refresh -> refresh()
                NoticeDetailIntent.TogglePin -> togglePin()
                NoticeDetailIntent.Edit -> edit()
                is NoticeDetailIntent.SetDeleteDialog -> dispatch(NoticeDetailReducerEvent.DeleteDialog(intent.visible))
                NoticeDetailIntent.ConfirmDelete -> confirmDelete()
                NoticeDetailIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: NoticeDetailState,
            event: NoticeDetailReducerEvent,
        ): NoticeDetailState =
            when (event) {
                is NoticeDetailReducerEvent.Loading ->
                    state.copy(
                        isLoading = true,
                        challengeId = event.challengeId,
                        noticeId = event.noticeId,
                        canManage = event.canManage,
                        errorMessage = null,
                    )

                is NoticeDetailReducerEvent.Loaded ->
                    state.copy(isLoading = false, detail = event.detail, errorMessage = null)

                is NoticeDetailReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is NoticeDetailReducerEvent.DeleteDialog -> state.copy(showDeleteDialog = event.visible)

                is NoticeDetailReducerEvent.Mutating -> state.copy(isMutating = event.mutating)

                is NoticeDetailReducerEvent.PinChanged ->
                    state.copy(detail = state.detail?.copy(pinned = event.pinned))
            }

        private fun load(
            challengeId: String,
            noticeId: String,
            canManage: Boolean,
        ) {
            dispatch(NoticeDetailReducerEvent.Loading(challengeId, noticeId, canManage))
            fetch(challengeId, noticeId)
        }

        private fun refresh() {
            val state = currentState
            if (state.noticeId.isBlank()) return
            fetch(state.challengeId, state.noticeId)
        }

        private fun fetch(
            challengeId: String,
            noticeId: String,
        ) {
            viewModelScope.launch {
                runCatching { getNoticeDetailUseCase(challengeId, noticeId) }
                    .onSuccess { dispatch(NoticeDetailReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(NoticeDetailReducerEvent.Failed(it.message ?: "공지를 불러오지 못했어요")) }
            }
        }

        private fun togglePin() {
            val detail = currentState.detail ?: return
            if (currentState.isMutating) return
            viewModelScope.launch {
                dispatch(NoticeDetailReducerEvent.Mutating(true))
                runCatching {
                    pinNoticeUseCase(
                        challengeId = currentState.challengeId,
                        noticeId = currentState.noticeId,
                        pinned = !detail.pinned,
                    )
                }.onSuccess { result ->
                    dispatch(NoticeDetailReducerEvent.PinChanged(result.pinned))
                    emitEffect(
                        NoticeDetailEffect.ShowMessage(
                            if (result.pinned) "공지를 고정했어요" else "고정을 해제했어요",
                        ),
                    )
                }.onFailure {
                    emitEffect(NoticeDetailEffect.ShowMessage(it.message ?: "고정 상태를 바꾸지 못했어요"))
                }
                dispatch(NoticeDetailReducerEvent.Mutating(false))
            }
        }

        private fun edit() {
            if (!currentState.canManage) return
            navigationHelper.navigateByRoute(
                ChallengeNoticeEditPage(
                    challengeId = currentState.challengeId,
                    noticeId = currentState.noticeId,
                ).toRoute(),
            )
        }

        private fun confirmDelete() {
            if (currentState.isMutating) return
            viewModelScope.launch {
                dispatch(NoticeDetailReducerEvent.Mutating(true))
                runCatching { deleteNoticeUseCase(currentState.challengeId, currentState.noticeId) }
                    .onSuccess {
                        emitEffect(NoticeDetailEffect.ShowMessage("공지를 삭제했어요"))
                        navigationHelper.navigateToBack()
                    }.onFailure {
                        emitEffect(NoticeDetailEffect.ShowMessage(it.message ?: "공지를 삭제하지 못했어요"))
                    }
                dispatch(NoticeDetailReducerEvent.Mutating(false))
                dispatch(NoticeDetailReducerEvent.DeleteDialog(false))
            }
        }
    }
