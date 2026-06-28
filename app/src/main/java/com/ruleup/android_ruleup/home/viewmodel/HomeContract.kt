package com.ruleup.android_ruleup.home.viewmodel

import com.ruleup.android_ruleup.home.HomeChallengeUi
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

/** 홈 카드 필터 탭. */
enum class HomeFilter {
    /** 진행 중 (전체 내 챌린지). */
    ACTIVE,

    /** 오늘 할 일 (오늘이 대상일인 챌린지). */
    TODAY,
}

sealed interface HomeIntent : MviIntent {
    data object Load : HomeIntent

    data object CreateChallenge : HomeIntent

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
    /** 진행 중 개수. */
    val activeCount: Int get() = challenges.size

    /** 오늘 할 일 개수. */
    val todayCount: Int get() = challenges.count { it.todayTarget }

    /** 현재 필터로 거른 카드. */
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
