package com.ruleup.verification.presentation.pending.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.ObjectionDecision
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 방장/공동 관리자 확인 대기함(명세 pending-reviews). 진입 시 목록을 조회하고,
 * 이의 제기 항목의 승인/기각(decideObjection)을 처리한 뒤 목록을 재조회한다.
 */
@HiltViewModel
class PendingReviewsViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<PendingReviewsIntent, PendingReviewsState, PendingReviewsReducerEvent, PendingReviewsEffect>(
            PendingReviewsState.initial,
        ) {
        private var challengeId: String? = null

        override fun onIntent(intent: PendingReviewsIntent) {
            when (intent) {
                is PendingReviewsIntent.Load -> {
                    challengeId = intent.challengeId
                    load(intent.challengeId)
                }

                is PendingReviewsIntent.Decide -> decide(intent.objectionId, intent.decision)
                PendingReviewsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: PendingReviewsState,
            event: PendingReviewsReducerEvent,
        ): PendingReviewsState =
            when (event) {
                PendingReviewsReducerEvent.Loading -> state.copy(isLoading = true, error = null)
                is PendingReviewsReducerEvent.Loaded -> state.copy(isLoading = false, reviews = event.reviews, error = null)
                is PendingReviewsReducerEvent.Failed -> state.copy(isLoading = false, error = event.message)
                is PendingReviewsReducerEvent.Deciding -> state.copy(isDeciding = event.deciding)
            }

        private fun load(id: String) {
            viewModelScope.launch {
                dispatch(PendingReviewsReducerEvent.Loading)
                runCatching { verificationRepository.getPendingReviews(id) }
                    .onSuccess { dispatch(PendingReviewsReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(PendingReviewsReducerEvent.Failed(it.message ?: "대기함을 불러오지 못했어요")) }
            }
        }

        private fun decide(
            objectionId: String,
            decision: ObjectionDecision,
        ) {
            val id = challengeId ?: return
            if (currentState.isDeciding) return
            viewModelScope.launch {
                dispatch(PendingReviewsReducerEvent.Deciding(true))
                runCatching { verificationRepository.decideObjection(id, objectionId, decision) }
                    .onSuccess {
                        val message = if (decision == ObjectionDecision.APPROVE) "이의 제기를 승인했어요" else "이의 제기를 기각했어요"
                        emitEffect(PendingReviewsEffect.ShowMessage(message))
                        load(id)
                    }.onFailure {
                        emitEffect(PendingReviewsEffect.ShowMessage(it.message ?: "처리에 실패했어요"))
                    }
                dispatch(PendingReviewsReducerEvent.Deciding(false))
            }
        }
    }
