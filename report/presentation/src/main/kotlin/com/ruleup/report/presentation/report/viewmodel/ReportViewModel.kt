package com.ruleup.report.presentation.report.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.report.domain.entity.ReportContext
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.domain.entity.ReportTarget
import com.ruleup.report.domain.repository.ReportRepository
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 신고하기. */
@HiltViewModel
class ReportViewModel
    @Inject
    constructor(
        private val reportRepository: ReportRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ReportIntent, ReportState, ReportReducerEvent, ReportEffect>(ReportState.initial) {
        private var userId: String? = null
        private var challengeId: String? = null

        override fun onIntent(intent: ReportIntent) {
            when (intent) {
                is ReportIntent.Init -> init(intent)
                is ReportIntent.SelectReason -> dispatch(ReportReducerEvent.ReasonSelected(intent.reason))
                ReportIntent.Submit -> submit()
                ReportIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ReportState,
            event: ReportReducerEvent,
        ): ReportState =
            when (event) {
                is ReportReducerEvent.TargetResolved ->
                    state.copy(targetName = event.targetName, reasons = event.reasons)

                is ReportReducerEvent.ReasonSelected -> state.copy(selected = event.reason)
                is ReportReducerEvent.Submitting -> state.copy(isSubmitting = event.inProgress)
                is ReportReducerEvent.Done -> state.copy(isSubmitting = false, done = event.effect)
            }

        /** 신고 대상 확정. */
        private fun init(intent: ReportIntent.Init) {
            userId = intent.userId?.takeIf { it.isNotBlank() }
            challengeId = intent.challengeId?.takeIf { it.isNotBlank() }
            dispatch(
                ReportReducerEvent.TargetResolved(
                    targetName = intent.targetName,
                    reasons = if (userId != null) ReportReason.forUser else ReportReason.forChallenge,
                ),
            )
        }

        private fun submit() {
            val reason = currentState.selected ?: return
            if (currentState.isSubmitting) return
            val target = target(reason) ?: return
            dispatch(ReportReducerEvent.Submitting(true))
            viewModelScope.launch {
                runCatching { reportRepository.report(target) }
                    .onSuccess { dispatch(ReportReducerEvent.Done(it.hiddenEffect)) }
                    .onFailure {
                        dispatch(ReportReducerEvent.Submitting(false))
                        val failure = (it as? com.ruleup.report.domain.entity.ReportException)?.failure
                        emitEffect(
                            ReportEffect.ShowMessage(
                                if (failure ==
                                    com.ruleup.report.domain.entity.ReportFailure.ALREADY_REPORTED
                                ) {
                                    "이미 신고한 대상이에요."
                                } else {
                                    "신고를 접수하지 못했어요. 다시 시도해 주세요."
                                },
                            ),
                        )
                        if (failure == com.ruleup.report.domain.entity.ReportFailure.ALREADY_REPORTED) navigationHelper.navigateToBack()
                    }
            }
        }

        /** 진입점이 프로필이면 발생한 챌린지가 없어도 된다 */
        private fun target(reason: ReportReason): ReportTarget? {
            val user = userId
            val challenge = challengeId
            return when {
                user != null ->
                    ReportTarget.User(
                        userId = user,
                        reason = reason,
                        context = if (challenge == null) ReportContext.PROFILE else ReportContext.ROOM,
                        challengeId = challenge,
                    )

                challenge != null ->
                    ReportTarget.Challenge(
                        challengeId = challenge,
                        reason = reason,
                        context = ReportContext.CHALLENGE_DETAIL,
                    )

                else -> null
            }
        }

        /** 접수가 끝나면 이 화면은 할 일이 없다 */
        fun onDoneDismissed() {
            navigationHelper.navigateToBack()
        }
    }
