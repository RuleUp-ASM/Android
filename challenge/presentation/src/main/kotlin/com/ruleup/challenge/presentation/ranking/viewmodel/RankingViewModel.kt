package com.ruleup.challenge.presentation.ranking.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.usecase.GetChallengeRankingUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 그룹 랭킹 ViewModel. 정렬(progressRate → successDays → joinedAt)은 서버가 확정해 내려주므로
 * 받은 순서 그대로 렌더링한다. 기간 탭(시즌제)은 Phase 2 — 현재 API 에 파라미터가 없다.
 */
@HiltViewModel
class RankingViewModel
    @Inject
    constructor(
        private val getChallengeRankingUseCase: GetChallengeRankingUseCase,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<RankingIntent, RankingState, RankingReducerEvent, NoEffect>(
            RankingState.initial,
        ) {
        override fun onIntent(intent: RankingIntent) {
            when (intent) {
                is RankingIntent.Load -> load(intent.challengeId)
                RankingIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: RankingState,
            event: RankingReducerEvent,
        ): RankingState =
            when (event) {
                is RankingReducerEvent.Loading ->
                    state.copy(isLoading = true, challengeId = event.challengeId, errorMessage = null)

                is RankingReducerEvent.Loaded ->
                    state.copy(isLoading = false, ranking = event.ranking, errorMessage = null)

                is RankingReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)
            }

        private fun load(challengeId: String) {
            dispatch(RankingReducerEvent.Loading(challengeId))
            viewModelScope.launch {
                runCatching { getChallengeRankingUseCase(challengeId) }
                    .onSuccess { dispatch(RankingReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(RankingReducerEvent.Failed(it.message ?: "랭킹을 불러오지 못했어요")) }
            }
        }
    }
