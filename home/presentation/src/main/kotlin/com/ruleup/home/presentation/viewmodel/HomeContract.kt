package com.ruleup.home.presentation.viewmodel

import com.ruleup.home.presentation.HomeChallengeUi
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

enum class HomeFilter {
    /** 진행 중 (전체 내 챌린지). */
    ACTIVE,

    /** 오늘 할 일 (오늘이 대상일인 챌린지). */
    TODAY,
}

sealed interface HomeIntent : MviIntent {
    data object Load : HomeIntent

    data object CreateChallenge : HomeIntent

    /** 하단 탭 "챌린지" → 탐색 메인. */
    data object OpenExplore : HomeIntent

    data object OpenMy : HomeIntent

    data class OpenChallenge(
        val challengeId: String,
    ) : HomeIntent

    data class SelectFilter(
        val filter: HomeFilter,
    ) : HomeIntent
}

data class HomeState(
    val isLoading: Boolean,
    val challenges: List<HomeChallengeUi>,
    val filter: HomeFilter,
) : UiState {
    /** 챌린지가 하나도 없는 상태. 로딩 중에는 빈 상태를 띄우지 않는다 — 곧 채워질 화면에 "없어요"가 스쳐 지나간다. */
    val isEmpty: Boolean
        get() = !isLoading && challenges.isEmpty()

    val activeCount: Int get() = challenges.size

    val todayCount: Int get() = challenges.count { it.todayTarget }

    val visibleChallenges: List<HomeChallengeUi>
        get() =
            when (filter) {
                HomeFilter.ACTIVE -> challenges
                HomeFilter.TODAY -> challenges.filter { it.todayTarget }
            }

    companion object {
        val initial = HomeState(isLoading = true, challenges = emptyList(), filter = HomeFilter.ACTIVE)
    }
}

sealed interface HomeReducerEvent : ReducerEvent {
    data object Loading : HomeReducerEvent

    data class Loaded(
        val challenges: List<HomeChallengeUi>,
    ) : HomeReducerEvent

    data class FilterSelected(
        val filter: HomeFilter,
    ) : HomeReducerEvent
}
