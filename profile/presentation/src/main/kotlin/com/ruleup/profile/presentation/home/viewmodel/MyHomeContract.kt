package com.ruleup.profile.presentation.home.viewmodel

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.profile.domain.entity.StatsReport
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyHomeIntent : MviIntent {
    data object Load : MyHomeIntent

    /** 재진입(ON_RESUME) 시 재조회 */
    data object Refresh : MyHomeIntent

    data object OpenProfileEdit : MyHomeIntent

    /** 티어 카드의 「자세히」 */
    data object OpenTier : MyHomeIntent

    /** 메뉴: 인증 기록 */
    data object OpenCalendar : MyHomeIntent

    data object OpenAppeals : MyHomeIntent

    /** 메뉴: 그룹 랭킹 */
    data object OpenRanking : MyHomeIntent

    data class SelectPickedChallenge(
        val challengeId: String,
    ) : MyHomeIntent

    data object DismissChallengePicker : MyHomeIntent

    data object OpenStats : MyHomeIntent

    data object OpenInvite : MyHomeIntent

    /** 메뉴: 신고한 사용자·챌린지. */
    data object OpenBlocks : MyHomeIntent

    /** 메뉴: 감시자 */
    data object OpenWatchers : MyHomeIntent

    /** 메뉴: 알림 설정. */
    data object OpenNotificationSettings : MyHomeIntent

    /** 알림함 */
    data object OpenNotificationCenter : MyHomeIntent

    /** 메뉴: 계정 · 약관 */
    data object OpenSettings : MyHomeIntent

    data object OpenHomeTab : MyHomeIntent

    /** 하단 탭: 탐색으로 전환. */
    data object OpenChallengeTab : MyHomeIntent

    /** 하단 탭: 내 챌린지(진행 중 / 완료·이탈). */
    data object OpenMyChallengesTab : MyHomeIntent

    data object CreateChallenge : MyHomeIntent
}

/** 선택 시트가 무엇을 고르는 중인지. */
enum class ChallengePickerTarget {
    RANKING,
    WATCHERS,
}

data class ChallengePicker(
    val target: ChallengePickerTarget,
    val challenges: List<GroupChallengeSummary>,
)

sealed interface MyHomeEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : MyHomeEffect
}

data class MyHomeState(
    val isLoading: Boolean,
    val home: MyHome?,
    // 전체 성공률 카드용.
    val stats: StatsReport?,
    val errorMessage: String?,
    // 챌린지 선택 시트 (null = 닫힘).
    val picker: ChallengePicker? = null,
    val isLoadingPicker: Boolean = false,
) : UiState {
    companion object {
        val initial =
            MyHomeState(
                isLoading = true,
                home = null,
                stats = null,
                errorMessage = null,
            )
    }
}

sealed interface MyHomeReducerEvent : ReducerEvent {
    data object Loading : MyHomeReducerEvent

    data class Loaded(
        val home: MyHome,
    ) : MyHomeReducerEvent

    /** 통계는 마이 홈의 필수 데이터가 아니다 */
    data class StatsLoaded(
        val stats: StatsReport,
    ) : MyHomeReducerEvent

    data class Failed(
        val message: String,
    ) : MyHomeReducerEvent

    data class LoadingPicker(
        val loading: Boolean,
    ) : MyHomeReducerEvent

    /** 참여 중 챌린지 2개 이상 */
    data class PickerShown(
        val picker: ChallengePicker,
    ) : MyHomeReducerEvent

    data object PickerDismissed : MyHomeReducerEvent
}
