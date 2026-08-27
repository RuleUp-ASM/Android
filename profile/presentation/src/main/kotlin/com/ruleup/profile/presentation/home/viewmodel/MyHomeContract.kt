package com.ruleup.profile.presentation.home.viewmodel

import com.ruleup.profile.domain.entity.GroupChallengeSummary
import com.ruleup.profile.domain.entity.MyHome
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface MyHomeIntent : MviIntent {
    data object Load : MyHomeIntent

    /** 재진입(ON_RESUME) 시 재조회 — 프로필 편집·챌린지 완주 등으로 값이 바뀐다. */
    data object Refresh : MyHomeIntent

    data object OpenProfileEdit : MyHomeIntent

    data object OpenTemperature : MyHomeIntent

    data object OpenCalendar : MyHomeIntent

    data object OpenAppeals : MyHomeIntent

    /** 메뉴: 그룹 랭킹 — 참여 중 그룹 챌린지를 골라 랭킹 화면으로 (1개면 바로 이동). */
    data object OpenRanking : MyHomeIntent

    data class SelectRankingChallenge(
        val challengeId: String,
    ) : MyHomeIntent

    data object DismissRankingPicker : MyHomeIntent

    data object OpenStats : MyHomeIntent

    data object OpenInvite : MyHomeIntent

    /** 메뉴: 설정 — 스펙 범위 밖이라 진입점만(준비 중 안내). */
    data object OpenSettings : MyHomeIntent

    data object OpenHomeTab : MyHomeIntent

    /** 하단 탭: 챌린지(탐색)로 전환. */
    data object OpenChallengeTab : MyHomeIntent
}

sealed interface MyHomeEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : MyHomeEffect
}

data class MyHomeState(
    val isLoading: Boolean,
    val home: MyHome?,
    val errorMessage: String?,
    // 그룹 랭킹 진입용 챌린지 선택 시트 (null = 닫힘)
    val rankingPicker: List<GroupChallengeSummary>? = null,
    val isLoadingRanking: Boolean = false,
) : UiState {
    companion object {
        val initial =
            MyHomeState(
                isLoading = true,
                home = null,
                errorMessage = null,
            )
    }
}

sealed interface MyHomeReducerEvent : ReducerEvent {
    data object Loading : MyHomeReducerEvent

    data class Loaded(
        val home: MyHome,
    ) : MyHomeReducerEvent

    data class Failed(
        val message: String,
    ) : MyHomeReducerEvent

    data class LoadingRanking(
        val loading: Boolean,
    ) : MyHomeReducerEvent

    /** 그룹 챌린지 2개 이상 — 선택 시트 노출. */
    data class RankingPickerShown(
        val challenges: List<GroupChallengeSummary>,
    ) : MyHomeReducerEvent

    data object RankingPickerDismissed : MyHomeReducerEvent
}
