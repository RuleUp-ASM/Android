package com.ruleup.challenge.presentation.detail.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.ChallengeTargetsPage
import com.ruleup.challenge.domain.TargetAppStore
import com.ruleup.challenge.domain.usecase.GetChallengeDetailUseCase
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.ui.mvi.NoEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 챌린지 상세/참여 ViewModel.
 *
 * 상세를 조회해 렌더하고, "참여하기"의 권한 확보가 끝나면([Proceed]) 좌표 바인딩 화면으로 이동한다.
 * 권한 확인/요청·모달 노출은 Context 가 필요해 화면(Composable)이 담당한다.
 */
@HiltViewModel
class ChallengeDetailViewModel
    @Inject
    constructor(
        private val getChallengeDetailUseCase: GetChallengeDetailUseCase,
        private val targetAppStore: TargetAppStore,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ChallengeDetailIntent, ChallengeDetailState, ChallengeDetailReducerEvent, NoEffect>(
            ChallengeDetailState.initial,
        ) {
        override fun onIntent(intent: ChallengeDetailIntent) {
            when (intent) {
                is ChallengeDetailIntent.Load -> load(intent.challengeId)
                ChallengeDetailIntent.RefreshSetup -> refreshSetup()
                ChallengeDetailIntent.RegisterApps -> registerApps()
                ChallengeDetailIntent.Proceed -> proceed()
                ChallengeDetailIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ChallengeDetailState,
            event: ChallengeDetailReducerEvent,
        ): ChallengeDetailState =
            when (event) {
                is ChallengeDetailReducerEvent.Loading ->
                    state.copy(isLoading = true, challengeId = event.challengeId, errorMessage = null)

                is ChallengeDetailReducerEvent.Loaded ->
                    state.copy(
                        isLoading = false,
                        detail = event.detail,
                        errorMessage = null,
                        targetAppsRegistered = event.targetAppsRegistered,
                    )

                is ChallengeDetailReducerEvent.Failed ->
                    state.copy(isLoading = false, errorMessage = event.message)

                is ChallengeDetailReducerEvent.SetupRefreshed ->
                    state.copy(targetAppsRegistered = event.targetAppsRegistered)
            }

        private fun load(challengeId: String) {
            if (currentState.detail?.challengeId == challengeId) return
            viewModelScope.launch {
                dispatch(ChallengeDetailReducerEvent.Loading(challengeId))
                runCatching { getChallengeDetailUseCase(challengeId) }
                    .onSuccess {
                        dispatch(
                            ChallengeDetailReducerEvent.Loaded(
                                detail = it,
                                targetAppsRegistered = targetAppStore.isRegistered(challengeId),
                            ),
                        )
                    }.onFailure { dispatch(ChallengeDetailReducerEvent.Failed(it.message ?: "챌린지를 불러오지 못했어요")) }
            }
        }

        // 앱 등록 화면에서 돌아왔을 때 등록 상태를 재확인해 버튼 모드를 갱신한다.
        private fun refreshSetup() {
            val id = currentState.detail?.challengeId ?: return
            dispatch(ChallengeDetailReducerEvent.SetupRefreshed(targetAppStore.isRegistered(id)))
        }

        private fun registerApps() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            navigationHelper.navigateByRoute(ChallengeTargetsPage(id).toRoute())
        }

        private fun proceed() {
            val id = currentState.detail?.challengeId ?: currentState.challengeId
            if (id.isBlank()) return
            // GPS 루틴 좌표 바인딩(verification/location) 으로 이동. 참여 API 가 memberId 를 돌려주지 않아
            // verification 의 기존 POC 관례대로 challengeMemberId 자리에 challengeId 를 넣는다.
            navigationHelper.navigateByRoute(
                NavRoute(
                    AppRoutes.VERIFICATION_LOCATION,
                    mapOf(
                        "challengeMemberId" to id,
                        "defaultRadiusM" to "100.0",
                        "dwellMinutes" to "60",
                    ),
                ),
            )
        }
    }
