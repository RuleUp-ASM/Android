package com.ruleup.challenge.presentation.notice.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.navigation.ChallengeNoticeDetailPage
import com.ruleup.challenge.domain.navigation.ChallengeNoticeEditPage
import com.ruleup.challenge.domain.usecase.GetNoticesUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 공지 목록 ViewModel. 정렬(고정 우선 → 최신순)·건수(최근 10건)는 서버 고정이라
 * 받은 순서 그대로 렌더링한다. 읽음 여부는 항목의 isRead 로 표시만 한다(처리는 상세 조회가 담당).
 */
@HiltViewModel
class NoticeListViewModel
    @Inject
    constructor(
        private val getNoticesUseCase: GetNoticesUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<NoticeListIntent, NoticeListState, NoticeListReducerEvent, NoEffect>(
            NoticeListState.initial,
        ) {
        override fun onIntent(intent: NoticeListIntent) {
            when (intent) {
                is NoticeListIntent.Load -> load(intent.challengeId, intent.canManage)
                NoticeListIntent.Refresh -> refresh()
                is NoticeListIntent.OpenNotice -> openNotice(intent.noticeId)
                NoticeListIntent.CreateNotice -> createNotice()
                NoticeListIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: NoticeListState,
            event: NoticeListReducerEvent,
        ): NoticeListState =
            when (event) {
                is NoticeListReducerEvent.Loading ->
                    state.copy(
                        isLoading = true,
                        challengeId = event.challengeId,
                        canManage = event.canManage,
                        errorMessage = null,
                    )

                is NoticeListReducerEvent.Loaded ->
                    state.copy(isLoading = false, notices = event.notices, errorMessage = null)

                is NoticeListReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load(
            challengeId: String,
            canManage: Boolean,
        ) {
            dispatch(NoticeListReducerEvent.Loading(challengeId, canManage))
            fetch(challengeId)
        }

        // 상세(읽음)·작성·수정에서 돌아오면 목록 상태가 바뀌므로 매번 재조회한다.
        private fun refresh() {
            val id = currentState.challengeId
            if (id.isBlank()) return
            fetch(id)
        }

        private fun fetch(challengeId: String) {
            viewModelScope.launch {
                runCatching { getNoticesUseCase(challengeId) }
                    .onSuccess { dispatch(NoticeListReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(NoticeListReducerEvent.Failed(it.message ?: "공지를 불러오지 못했어요")) }
            }
        }

        private fun openNotice(noticeId: String) {
            navigationHelper.navigateByRoute(
                ChallengeNoticeDetailPage(
                    challengeId = currentState.challengeId,
                    noticeId = noticeId,
                    canManage = currentState.canManage,
                ).toRoute(),
            )
        }

        private fun createNotice() {
            if (!currentState.canManage) return
            navigationHelper.navigateByRoute(
                ChallengeNoticeEditPage(challengeId = currentState.challengeId).toRoute(),
            )
        }
    }
