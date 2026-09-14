package com.ruleup.verification.presentation.manual.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.CancelWindowClosedException
import com.ruleup.verification.domain.entity.InvalidTargetDateException
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 수동 인증 제출 ViewModel (명세 POST/DELETE `/challenges/{id}/verifications`).
 *
 * **수동 방에서만 열린다.** 자동 방에 대고 제출하면 서버가 `NOT_MANUAL_CHALLENGE`(409)로 막으므로,
 * 진입점을 두지 않는 것이 전제다.
 *
 * 제목은 진행률에서, 오늘 상태는 today 조회에서 온다. 둘을 함께 던지되 **제목이 실패해도 화면은
 * 세운다** — 체크에 필요한 것은 challengeId 하나뿐인데 제목 때문에 오늘 인증을 막을 이유가 없다.
 */
@HiltViewModel
class ManualSubmitViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ManualSubmitIntent, ManualSubmitState, ManualSubmitReducerEvent, NoEffect>(
            ManualSubmitState.initial(""),
        ) {
        override fun onIntent(intent: ManualSubmitIntent) {
            when (intent) {
                is ManualSubmitIntent.Load -> load(intent.challengeId)
                ManualSubmitIntent.Retry -> load(currentState.challengeId)
                ManualSubmitIntent.Back -> navigationHelper.navigateToBack()
                is ManualSubmitIntent.NoteChanged -> dispatch(ManualSubmitReducerEvent.NoteEdited(intent.value))
                ManualSubmitIntent.Submit -> submit()
                ManualSubmitIntent.Uncheck -> uncheck()
            }
        }

        override fun reduce(
            state: ManualSubmitState,
            event: ManualSubmitReducerEvent,
        ): ManualSubmitState =
            when (event) {
                is ManualSubmitReducerEvent.Loading ->
                    state.copy(
                        challengeId = event.challengeId,
                        isLoading = !event.silent,
                        errorMessage = if (event.silent) state.errorMessage else null,
                    )

                is ManualSubmitReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        title = event.title,
                        date = event.date,
                        window = event.window,
                        status = event.status,
                        verificationId = event.verificationId,
                        streakAfter = event.streakAfter,
                        errorMessage = if (event.keepMessage) state.errorMessage else null,
                    )

                is ManualSubmitReducerEvent.Failed ->
                    state.copy(isLoading = false, isSubmitting = false, errorMessage = event.message)

                is ManualSubmitReducerEvent.NoteEdited -> state.copy(note = event.value, errorMessage = null)

                is ManualSubmitReducerEvent.Submitting ->
                    state.copy(isSubmitting = event.submitting, errorMessage = null)

                is ManualSubmitReducerEvent.Submitted ->
                    state.copy(
                        isSubmitting = false,
                        verificationId = event.verificationId,
                        status = event.status,
                        streakAfter = event.streakAfter,
                        errorMessage = null,
                    )

                ManualSubmitReducerEvent.Unchecked ->
                    state.copy(
                        isSubmitting = false,
                        verificationId = null,
                        status = null,
                        errorMessage = null,
                    )
            }

        private fun load(
            challengeId: String,
            silent: Boolean = false,
        ) {
            if (challengeId.isBlank()) {
                // 인자 없이 열린 화면이다. 서버를 불러 봐야 어느 챌린지인지 말할 수 없다.
                dispatch(ManualSubmitReducerEvent.Failed("어떤 챌린지인지 알 수 없어요"))
                return
            }
            dispatch(ManualSubmitReducerEvent.Loading(challengeId, silent))
            viewModelScope.launch {
                val (today, title) =
                    coroutineScope {
                        val t = async { runCatching { verificationRepository.getTodayResult(challengeId) } }
                        // 제목만 쓰려고 진행률을 부른다. 실패는 흡수한다 — 제목이 없어도 체크는 된다.
                        val p =
                            async {
                                runCatching { verificationRepository.getProgress() }
                                    .getOrNull()
                                    ?.challenges
                                    ?.firstOrNull { it.challengeId == challengeId }
                                    ?.title
                            }
                        t.await() to p.await()
                    }

                today
                    .onSuccess { result ->
                        dispatch(
                            ManualSubmitReducerEvent.Loaded(
                                title = title.orEmpty(),
                                date = result.date,
                                window = result.window,
                                status = result.status,
                                verificationId = result.verificationId,
                                streakAfter = result.streak?.after,
                                keepMessage = silent,
                            ),
                        )
                    }.onFailure {
                        dispatch(ManualSubmitReducerEvent.Failed(it.userMessage("오늘 상태를 불러오지 못했어요")))
                    }
            }
        }

        private fun submit() {
            val state = currentState
            if (!state.canSubmit) return
            dispatch(ManualSubmitReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching {
                    verificationRepository.submitManual(
                        challengeId = state.challengeId,
                        // 오늘로 서버가 잡게 둔다 — 기기 시계를 믿고 날짜를 보내면 자정 근처에서 어긋난다.
                        note = state.note.trim().takeIf { it.isNotEmpty() },
                    )
                }.onSuccess { result ->
                    dispatch(
                        ManualSubmitReducerEvent.Submitted(
                            verificationId = result.verificationId,
                            status = result.status,
                            streakAfter = result.streak?.after,
                        ),
                    )
                }.onFailure {
                    dispatch(ManualSubmitReducerEvent.Failed(it.userMessage("오늘 인증을 체크하지 못했어요")))
                    // 이미 체크된 경우가 섞여 있어 서버 상태를 다시 받는다.
                    load(state.challengeId, silent = true)
                }
            }
        }

        private fun uncheck() {
            val state = currentState
            val verificationId = state.verificationId ?: return
            if (!state.canUncheck) return
            dispatch(ManualSubmitReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { verificationRepository.cancelManual(verificationId) }
                    .onSuccess { dispatch(ManualSubmitReducerEvent.Unchecked) }
                    .onFailure {
                        dispatch(ManualSubmitReducerEvent.Failed(it.userMessage("체크를 해제하지 못했어요")))
                        load(state.challengeId, silent = true)
                    }
            }
        }
    }

/**
 * 실패를 사용자 문구로 옮긴다.
 *
 * 이미 인증한 날은 **오류가 아니라 안내**다 — 화면은 그 뒤 서버 상태를 다시 받아 체크된 모습으로
 * 바뀐다. 오류처럼 말하면 사용자가 인증이 안 된 줄 알고 다시 누른다.
 */
private fun Throwable.userMessage(fallback: String): String =
    when (this) {
        is AlreadyVerifiedException -> "오늘은 이미 체크했어요"
        is InvalidTargetDateException -> "오늘이 지나 체크할 수 없어요"
        is CancelWindowClosedException -> "오늘이 지나 해제할 수 없어요"
        else -> message?.takeIf { it.isNotBlank() } ?: fallback
    }
