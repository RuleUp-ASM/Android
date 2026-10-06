package com.ruleup.home.presentation.viewmodel

import com.ruleup.home.presentation.HomeChallengeUi
import com.ruleup.home.presentation.pickHero
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
}
