package com.ruleup.home.presentation.viewmodel

import com.ruleup.home.presentation.HomeChallengeUi
import com.ruleup.profile.domain.entity.CalendarDayStatus
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

    /** 상단 벨 → 알림 센터. */
    data object OpenNotifications : HomeIntent

    /** 하단 탭: 내 챌린지(진행 중 / 완료·이탈). */
    data object OpenMyChallenges : HomeIntent

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
    /** 읽지 않은 알림이 있는가 */
    val hasUnreadNotifications: Boolean = false,
    /** 이번 주 날짜(YYYY-MM-DD)별 판정. 판정 대상이 아닌 날은 없다. */
    val weekStatuses: Map<String, CalendarDayStatus> = emptyMap(),
) : UiState {
    /** 챌린지가 하나도 없는 상태. */
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

    /** 미읽음 집계는 홈의 부수 정보다 */
    data class UnreadChecked(
        val hasUnread: Boolean,
    ) : HomeReducerEvent

    data class WeekLoaded(
        val statuses: Map<String, CalendarDayStatus>,
    ) : HomeReducerEvent
}
