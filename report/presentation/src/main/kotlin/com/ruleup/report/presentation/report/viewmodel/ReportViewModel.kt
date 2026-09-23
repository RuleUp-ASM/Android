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

/**
 * 신고하기 (명세 POST /reports · Figma `1466:96`).
 *
 * **자유 텍스트를 받지 않는다.** 명세가 2026-08-26 개편에서 `detail` 을 폐기하고 "클라이언트에서
 * 입력란 자체를 제거한다"고 정했다. 디자인에는 입력칸이 남아 있으나, 서버가 받지 않는 값을 받으면
 * 사용자는 쓴 글이 전달됐다고 믿는다.
 *
 * 사유 목록은 대상이 정한다 — 챌린지에는 부정 인증 의심이 없고, 그 제약은 `ReportReason` 이 이미
 * 갖고 있다. 화면이 다시 추리지 않는다.
 */
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

        /** 신고 대상 확정. 사유 목록은 대상 종류로 갈린다 — 챌린지에는 없는 사유가 사용자에게 있다. */
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
                        emitEffect(ReportEffect.ShowMessage(it.message ?: "신고를 접수하지 못했어요"))
                    }
            }
        }

        /**
         * 진입점이 프로필이면 발생한 챌린지가 없어도 된다 — 그 외에는 명세가 챌린지를 요구한다.
         * 규칙 자체는 [ReportTarget] 이 `init` 에서 막으므로 여기서 다시 검사하지 않는다.
         */
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

        /** 접수가 끝나면 이 화면은 할 일이 없다 — 대상이 이미 가려져 돌아갈 자리도 사라진다. */
        fun onDoneDismissed() {
            navigationHelper.navigateToBack()
        }
    }
