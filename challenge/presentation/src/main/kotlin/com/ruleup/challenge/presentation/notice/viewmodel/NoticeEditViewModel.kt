package com.ruleup.challenge.presentation.notice.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.NoticePolicy
import com.ruleup.challenge.domain.usecase.CreateNoticeUseCase
import com.ruleup.challenge.domain.usecase.GetNoticeDetailUseCase
import com.ruleup.challenge.domain.usecase.UpdateNoticeUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 공지 작성/수정 ViewModel (방장 전용 진입).
 * 길이 상한은 [NoticePolicy] 로 선차단(입력 컷)하고 서버가 재검증한다(400 INVALID_NOTICE_PAYLOAD).
 * 수정 시 기본은 읽음 유지 — 재확인이 필요한 변경만 resetRead=true(미읽음 복귀 + 재발송).
 */
@HiltViewModel
class NoticeEditViewModel
    @Inject
    constructor(
        private val getNoticeDetailUseCase: GetNoticeDetailUseCase,
        private val createNoticeUseCase: CreateNoticeUseCase,
        private val updateNoticeUseCase: UpdateNoticeUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<NoticeEditIntent, NoticeEditState, NoticeEditReducerEvent, NoticeEditEffect>(
            NoticeEditState.initial,
        ) {
        override fun onIntent(intent: NoticeEditIntent) {
            when (intent) {
                is NoticeEditIntent.Load -> load(intent.challengeId, intent.noticeId)
                is NoticeEditIntent.ChangeTitle ->
                    dispatch(NoticeEditReducerEvent.TitleChanged(intent.title.take(NoticePolicy.TITLE_MAX_LENGTH)))

                is NoticeEditIntent.ChangeContent ->
                    dispatch(NoticeEditReducerEvent.ContentChanged(intent.content.take(NoticePolicy.CONTENT_MAX_LENGTH)))

                is NoticeEditIntent.TogglePinned -> dispatch(NoticeEditReducerEvent.PinnedChanged(intent.pinned))
                is NoticeEditIntent.ToggleResetRead -> dispatch(NoticeEditReducerEvent.ResetReadChanged(intent.resetRead))
                NoticeEditIntent.Save -> save()
                NoticeEditIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: NoticeEditState,
            event: NoticeEditReducerEvent,
        ): NoticeEditState =
            when (event) {
                is NoticeEditReducerEvent.Init ->
                    NoticeEditState.initial.copy(
                        challengeId = event.challengeId,
                        noticeId = event.noticeId,
                        isLoading = event.noticeId != null,
                    )

                is NoticeEditReducerEvent.Prefilled ->
                    state.copy(isLoading = false, title = event.title, content = event.content)

                is NoticeEditReducerEvent.PrefillFailed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is NoticeEditReducerEvent.TitleChanged -> state.copy(title = event.title)

                is NoticeEditReducerEvent.ContentChanged -> state.copy(content = event.content)

                is NoticeEditReducerEvent.PinnedChanged -> state.copy(pinned = event.pinned)

                is NoticeEditReducerEvent.ResetReadChanged -> state.copy(resetRead = event.resetRead)

                is NoticeEditReducerEvent.Saving -> state.copy(isSaving = event.saving)
            }

        private fun load(
            challengeId: String,
            noticeId: String?,
        ) {
            // 중복 Load(재구독 등)로 입력 중인 폼을 날리지 않는다.
            if (currentState.challengeId == challengeId && currentState.noticeId == noticeId) return
            dispatch(NoticeEditReducerEvent.Init(challengeId, noticeId))
            if (noticeId == null) return
            viewModelScope.launch {
                runCatching { getNoticeDetailUseCase(challengeId, noticeId) }
                    .onSuccess { dispatch(NoticeEditReducerEvent.Prefilled(title = it.title, content = it.content)) }
                    .onFailure { dispatch(NoticeEditReducerEvent.PrefillFailed(it.message ?: "공지를 불러오지 못했어요")) }
            }
        }

        private fun save() {
            val state = currentState
            if (state.isSaving) return
            if (state.title.isBlank() || state.content.isBlank()) {
                emitEffect(NoticeEditEffect.ShowMessage("제목과 내용을 입력해주세요"))
                return
            }
            viewModelScope.launch {
                dispatch(NoticeEditReducerEvent.Saving(true))
                runCatching {
                    val noticeId = state.noticeId
                    if (noticeId == null) {
                        createNoticeUseCase(
                            challengeId = state.challengeId,
                            title = state.title.trim(),
                            content = state.content.trim(),
                            pinned = state.pinned,
                        )
                    } else {
                        updateNoticeUseCase(
                            challengeId = state.challengeId,
                            noticeId = noticeId,
                            title = state.title.trim(),
                            content = state.content.trim(),
                            resetRead = state.resetRead,
                        )
                    }
                }.onSuccess {
                    emitEffect(
                        NoticeEditEffect.ShowMessage(
                            if (state.isEditMode) "공지를 수정했어요" else "공지를 등록했어요",
                        ),
                    )
                    navigationHelper.navigateToBack()
                }.onFailure {
                    emitEffect(NoticeEditEffect.ShowMessage(it.message ?: "공지를 저장하지 못했어요"))
                }
                dispatch(NoticeEditReducerEvent.Saving(false))
            }
        }
    }
