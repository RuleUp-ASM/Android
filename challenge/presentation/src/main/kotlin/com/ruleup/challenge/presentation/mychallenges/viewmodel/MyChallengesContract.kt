package com.ruleup.challenge.presentation.mychallenges.viewmodel

import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.notification.domain.entity.UnreadSummary
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.ProgressSnapshot

/**
 * 목록 세그먼트. 서버 `filter` 는 세 값이지만 화면은 **완료와 이탈을 한 탭에 합쳐** 보여준다
 * (Figma 1162:2) — 둘 다 "끝난 방"이고 사용자가 나눠 볼 이유가 없다.
 */
enum class MyChallengeSegment {
    IN_PROGRESS,
    FINISHED,
}

sealed interface MyChallengesIntent : MviIntent {
    data object Load : MyChallengesIntent

    /** 화면 복귀 — 진행 중 달성률이 그새 움직였을 수 있다. */
    data object Refresh : MyChallengesIntent

    data class SelectSegment(
        val segment: MyChallengeSegment,
    ) : MyChallengesIntent

    /** 목록 끝에 닿음 — 완료·이탈만 다음 장이 있다. */
    data object LoadMore : MyChallengesIntent

    data class OpenChallenge(
        val challengeId: String,
    ) : MyChallengesIntent

    /** 빈 상태의 「챌린지 둘러보기」. */
    data object OpenExplore : MyChallengesIntent

    data object OpenHomeTab : MyChallengesIntent

    data object OpenExploreTab : MyChallengesIntent

    data object OpenMyTab : MyChallengesIntent
}

sealed interface MyChallengesEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : MyChallengesEffect
}

/**
 * 완료와 이탈은 **서버에서 두 번 받아 합친다** — `filter` 가 둘을 나누기 때문이다.
 * 그래서 커서도 각각 따로 들고 있어야 한다.
 */
data class FinishedPaging(
    val completedCursor: String?,
    val completedHasNext: Boolean,
    val leftCursor: String?,
    val leftHasNext: Boolean,
) {
    val hasNext: Boolean
        get() = completedHasNext || leftHasNext

    companion object {
        val initial = FinishedPaging(null, true, null, true)
    }
}

data class MyChallengesState(
    val segment: MyChallengeSegment,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val inProgress: List<MyChallenge>,
    val finished: List<MyChallenge>,
    // 진행 중 카드의 달성률·D-day 원천. 없으면 두 값을 그리지 않는다
    val progress: ProgressSnapshot?,
    // 카드의 미읽음 뱃지(Figma 1314:2). 조회에 실패하면 빈 집계라 뱃지가 붙지 않는다
    val unread: UnreadSummary,
    val finishedPaging: FinishedPaging,
    val errorMessage: String?,
) : UiState {
    val current: List<MyChallenge>
        get() = if (segment == MyChallengeSegment.IN_PROGRESS) inProgress else finished

    companion object {
        val initial =
            MyChallengesState(
                segment = MyChallengeSegment.IN_PROGRESS,
                isLoading = true,
                isLoadingMore = false,
                inProgress = emptyList(),
                finished = emptyList(),
                progress = null,
                unread = UnreadSummary.EMPTY,
                finishedPaging = FinishedPaging.initial,
                errorMessage = null,
            )
    }
}

sealed interface MyChallengesReducerEvent : ReducerEvent {
    data object Loading : MyChallengesReducerEvent

    data class Loaded(
        val inProgress: List<MyChallenge>,
        val finished: List<MyChallenge>,
        val paging: FinishedPaging,
    ) : MyChallengesReducerEvent

    /** 진행률은 목록의 부수 정보라 실패해도 목록을 지우지 않는다. */
    data class ProgressLoaded(
        val progress: ProgressSnapshot,
    ) : MyChallengesReducerEvent

    /** 미읽음도 부수 정보다 — 못 세면 뱃지만 안 붙는다. */
    data class UnreadLoaded(
        val unread: UnreadSummary,
    ) : MyChallengesReducerEvent

    data class Failed(
        val message: String,
    ) : MyChallengesReducerEvent

    data class LoadingMore(
        val loading: Boolean,
    ) : MyChallengesReducerEvent

    data class MoreLoaded(
        val finished: List<MyChallenge>,
        val paging: FinishedPaging,
    ) : MyChallengesReducerEvent

    data class SegmentSelected(
        val segment: MyChallengeSegment,
    ) : MyChallengesReducerEvent
}
