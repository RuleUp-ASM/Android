package com.ruleup.profile.presentation.member.viewmodel

import com.ruleup.profile.domain.entity.MemberProfile
import com.ruleup.profile.presentation.common.SuspendedBlock
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MemberProfileIntent : MviIntent {
    /**
     * 화면 진입. **대상 userId 를 화면이 넘긴다** — 호스트는 라우트 인자를 컴포저블 파라미터로
     * 주지 `SavedStateHandle` 에 넣지 않아, ViewModel 이 거기서 읽으면 항상 비어 있다(REP-01).
     */
    data class Load(
        val userId: String,
    ) : MemberProfileIntent

    data object Retry : MemberProfileIntent

    data object Back : MemberProfileIntent

    data object Report : MemberProfileIntent

    data object DismissReportBlock : MemberProfileIntent

    /** 제재 이력으로 간다 — 사유와 해제일의 원본은 그 화면이다. */
    data object OpenSanctionHistory : MemberProfileIntent

    /** 차단 해제. **신고 취소가 아니다** — 신고 건과 스냅샷은 그대로 남는다. */
    data object Unblock : MemberProfileIntent
}

data class MemberProfileState(
    val isLoading: Boolean,
    val profile: MemberProfile?,
    val errorMessage: String?,
    val isOffline: Boolean,
    val isUnblocking: Boolean,
    // 신고 진입점은 숨기지 않는다 — 눌렀을 때 왜 막혔는지 시트로 말한다(제재 정책 §5.1).
    val reportBlock: SuspendedBlock?,
) : UiState {
    companion object {
        val initial =
            MemberProfileState(
                isLoading = true,
                profile = null,
                errorMessage = null,
                isOffline = false,
                isUnblocking = false,
                reportBlock = null,
            )
    }
}

sealed interface MemberProfileReducerEvent : ReducerEvent {
    data object Loading : MemberProfileReducerEvent

    data class Loaded(
        val profile: MemberProfile,
    ) : MemberProfileReducerEvent

    data class Failed(
        val message: String,
        val offline: Boolean,
    ) : MemberProfileReducerEvent

    data class Unblocking(
        val inProgress: Boolean,
    ) : MemberProfileReducerEvent

    data class ReportBlocked(
        val block: SuspendedBlock?,
    ) : MemberProfileReducerEvent
}

typealias MemberProfileEffect = NoEffect
