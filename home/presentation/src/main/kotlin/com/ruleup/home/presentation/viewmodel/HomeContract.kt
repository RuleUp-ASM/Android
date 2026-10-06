package com.ruleup.home.presentation.viewmodel

import com.ruleup.challenge.domain.entity.TrendingChallenge
import com.ruleup.domain.entity.category.Category
import com.ruleup.home.presentation.HomeChallengeUi
import com.ruleup.home.presentation.pickHero
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

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

    /** 신규 이용자: 관심 분야 칩 → 그 분야 둘러보기. */
    data class OpenCategory(
        val category: Category,
    ) : HomeIntent

    /** 주간 도장을 눌러 그날 결과를 본다. 오늘이나 이미 고른 날을 누르면 원래 홈으로. */
    data class SelectDay(
        val date: String,
    ) : HomeIntent

    data object RetryDay : HomeIntent
}

/** 고른 날의 결과. */
data class SelectedDay(
    // YYYY-MM-DD
    val date: String,
    val result: DayResult,
)

sealed interface DayResult {
    data object Loading : DayResult

    data class Loaded(
        val items: List<CalendarDayItem>,
    ) : DayResult

    data object Failed : DayResult
}

data class HomeState(
    val isLoading: Boolean,
    val challenges: List<HomeChallengeUi>,
    /** 읽지 않은 알림이 있는가 */
    val hasUnreadNotifications: Boolean = false,
    /** 이번 주 날짜(YYYY-MM-DD)별 판정. 판정 대상이 아닌 날은 없다. */
    val weekStatuses: Map<String, CalendarDayStatus> = emptyMap(),
    // 못 받으면 null — 인사만 띄운다
    val nickname: String? = null,
    /** 챌린지별 「오늘 직접 체크할 수 있는가」. 조회하지 않았거나 실패한 챌린지는 없다. */
    val manualCheckable: Map<String, Boolean> = emptyMap(),
    /** 챌린지가 없는 사람에게 보여 줄 첫 챌린지. 못 받으면 비어 있고 기본 안내를 띄운다. */
    val starters: List<TrendingChallenge> = emptyList(),
    val interests: List<Category> = emptyList(),
    // null 이면 오늘 — 히어로 · 매일 루틴 · 주 N회를 보여 준다
    val selectedDay: SelectedDay? = null,
) : UiState {
    /** 「오늘 해 볼까요?」에 올릴 챌린지. */
    val hero: HomeChallengeUi?
        get() = pickHero(challenges, manualCheckable)

    /** 챌린지가 하나도 없는 상태. */
    val isEmpty: Boolean
        get() = !isLoading && challenges.isEmpty()

    companion object {
        val initial = HomeState(isLoading = true, challenges = emptyList())
    }
}

sealed interface HomeReducerEvent : ReducerEvent {
    data object Loading : HomeReducerEvent

    data class Loaded(
        val challenges: List<HomeChallengeUi>,
    ) : HomeReducerEvent

    /** 미읽음 집계는 홈의 부수 정보다 */
    data class UnreadChecked(
        val hasUnread: Boolean,
    ) : HomeReducerEvent

    data class WeekLoaded(
        val statuses: Map<String, CalendarDayStatus>,
    ) : HomeReducerEvent

    data class NicknameLoaded(
        val nickname: String,
    ) : HomeReducerEvent

    data class CheckableLoaded(
        val manualCheckable: Map<String, Boolean>,
    ) : HomeReducerEvent

    data class DaySelected(
        val date: String?,
    ) : HomeReducerEvent

    data class DayLoaded(
        val date: String,
        val items: List<CalendarDayItem>,
    ) : HomeReducerEvent

    data class DayFailed(
        val date: String,
    ) : HomeReducerEvent

    data class StartersLoaded(
        val starters: List<TrendingChallenge>,
        val interests: List<Category>,
    ) : HomeReducerEvent
}
