package com.ruleup.challenge.presentation.ranking.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.repository.RoomRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 그룹 랭킹 ViewModel. */
@HiltViewModel
class RankingViewModel
    @Inject
    constructor(
        private val roomRepository: RoomRepository,
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
                runCatching { roomRepository.getRanking(challengeId) }
                    .onSuccess { dispatch(RankingReducerEvent.Loaded(it)) }
                    .onFailure { dispatch(RankingReducerEvent.Failed(it.userFacingMessage("랭킹을 불러오지 못했어요"))) }
            }
        }
    }
