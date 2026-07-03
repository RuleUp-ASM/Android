package com.ruleup.challenge.presentation.targets.viewmodel

import com.ruleup.analytics.AnalyticsEvent
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.challenge.domain.TargetAppStore
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 대상 앱 등록 ViewModel. 선택한 앱 패키지명을 [TargetAppStore] 에 저장해 등록 완료로 표시하고 종료한다.
 * 앱 목록 조회는 플랫폼(PackageManager) 의존이라 화면(Composable)이 담당하고, 여기선 저장만 처리한다.
 */
@HiltViewModel
class ChallengeTargetsViewModel
    @Inject
    constructor(
        private val targetAppStore: TargetAppStore,
        private val analyticsLogger: AnalyticsLogger,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<ChallengeTargetsIntent, ChallengeTargetsState, ChallengeTargetsReducerEvent, ChallengeTargetsEffect>(
            ChallengeTargetsState.initial,
        ) {
        override fun onIntent(intent: ChallengeTargetsIntent) {
            when (intent) {
                is ChallengeTargetsIntent.Save -> save(intent)
                ChallengeTargetsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ChallengeTargetsState,
            event: ChallengeTargetsReducerEvent,
        ): ChallengeTargetsState =
            when (event) {
                ChallengeTargetsReducerEvent.Saving -> state.copy(isSaving = true)
                ChallengeTargetsReducerEvent.Finished -> state.copy(isSaving = false)
            }

        private fun save(intent: ChallengeTargetsIntent.Save) {
            if (currentState.isSaving) return
            val packages = intent.packages.distinct()
            if (packages.isEmpty()) {
                emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱을 1개 이상 선택해주세요"))
                return
            }
            dispatch(ChallengeTargetsReducerEvent.Saving)
            targetAppStore.save(intent.challengeId, packages)
            dispatch(ChallengeTargetsReducerEvent.Finished)
            analyticsLogger.log(
                AnalyticsEvent.SetupStepCompleted(
                    step = AnalyticsEvent.SetupStepCompleted.STEP_TARGET_APPS,
                    challengeId = intent.challengeId,
                ),
            )
            emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱이 등록됐어요"))
            navigationHelper.navigateToBack()
        }
    }
