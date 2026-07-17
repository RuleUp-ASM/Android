package com.ruleup.challenge.presentation.ranking.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeRanking
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.NoEffect
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface RankingIntent : MviIntent {
    /** 화면 진입 시 랭킹 조회. */
    data class Load(
        val challengeId: String,
    ) : RankingIntent

    data object Back : RankingIntent
}

data class RankingState(
    val challengeId: String,
    val isLoading: Boolean,
    val ranking: ChallengeRanking?,
    val errorMessage: String?,
) : UiState {
    companion object {
        val initial =
            RankingState(
                challengeId = "",
                isLoading = true,
                ranking = null,
                errorMessage = null,
            )
    }
}

sealed interface RankingReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
    ) : RankingReducerEvent

    data class Loaded(
        val ranking: ChallengeRanking,
    ) : RankingReducerEvent

    data class Failed(
        val message: String,
    ) : RankingReducerEvent
}

/** 네비게이션은 NavigationHelper, 오류는 상태로 노출 — 단발성 이펙트 없음. */
typealias RankingEffect = NoEffect
