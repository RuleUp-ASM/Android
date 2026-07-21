package com.ruleup.challenge.presentation.targets.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.analytics.domain.AnalyticsEvent
import com.ruleup.analytics.domain.AnalyticsLogger
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.domain.challenge.ScreenAppBindingPort
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 대상 앱 등록 ViewModel. 진입 시 서버(my-screen-apps)에서 이전 선택을 복원하고, 저장 시 서버에
 * 바인딩한 뒤 로컬 [TargetAppStore](상세의 등록 게이트 판정용)에도 반영한다. 서버 호출은 core 포트
 * [ScreenAppBindingPort] 로 위임한다(feature 간 직접 의존 회피).
 */
@HiltViewModel
class ChallengeTargetsViewModel
    @Inject
    constructor(
        private val screenAppBindingPort: ScreenAppBindingPort,
        private val targetAppStore: TargetAppStore,
        private val analyticsLogger: AnalyticsLogger,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ChallengeTargetsIntent, ChallengeTargetsState, ChallengeTargetsReducerEvent, ChallengeTargetsEffect>(
            ChallengeTargetsState.initial,
        ) {
        override fun onIntent(intent: ChallengeTargetsIntent) {
            when (intent) {
                is ChallengeTargetsIntent.Load -> load(intent.challengeId)
                is ChallengeTargetsIntent.Save -> save(intent)
                ChallengeTargetsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ChallengeTargetsState,
            event: ChallengeTargetsReducerEvent,
        ): ChallengeTargetsState =
            when (event) {
                is ChallengeTargetsReducerEvent.Restored -> state.copy(restoredPackages = event.packages)
                ChallengeTargetsReducerEvent.Saving -> state.copy(isSaving = true)
                ChallengeTargetsReducerEvent.Finished -> state.copy(isSaving = false)
            }

        private fun load(challengeId: String) {
            viewModelScope.launch {
                // 미설정(null)·조회 실패는 조용히 무시 — 최초 진입이면 복원할 게 없다.
                val bound = runCatching { screenAppBindingPort.bound(challengeId) }.getOrNull().orEmpty()
                if (bound.isNotEmpty()) {
                    dispatch(ChallengeTargetsReducerEvent.Restored(bound.map { it.packageName }.toSet()))
                }
            }
        }

        private fun save(intent: ChallengeTargetsIntent.Save) {
            if (currentState.isSaving) return
            val apps = intent.apps.distinctBy { it.packageName }
            if (apps.isEmpty()) {
                emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱을 1개 이상 선택해주세요"))
                return
            }
            dispatch(ChallengeTargetsReducerEvent.Saving)
            viewModelScope.launch {
                runCatching { screenAppBindingPort.bind(intent.challengeId, apps) }
                    .onSuccess {
                        // 상세 화면의 "등록됨" 게이트 판정용 로컬 반영(서버 성공 시에만).
                        targetAppStore.save(intent.challengeId, apps.map { it.packageName })
                        analyticsLogger.log(
                            AnalyticsEvent.SetupStepCompleted(
                                step = AnalyticsEvent.SetupStepCompleted.STEP_TARGET_APPS,
                                challengeId = intent.challengeId,
                            ),
                        )
                        emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱이 등록됐어요"))
                        navigationHelper.navigateToBack()
                    }.onFailure {
                        emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱 저장에 실패했어요. 잠시 후 다시 시도해주세요"))
                    }
                dispatch(ChallengeTargetsReducerEvent.Finished)
            }
        }
    }
