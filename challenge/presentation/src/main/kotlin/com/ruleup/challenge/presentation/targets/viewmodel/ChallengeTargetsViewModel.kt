package com.ruleup.challenge.presentation.targets.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.InvalidScreenAppException
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.SettingChangeLimitException
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 대상 앱 등록 ViewModel. 진입 시 서버(my-screen-apps)에서 이전 선택을 복원하고, 저장 시 서버에
 * 바인딩한 뒤 로컬 [TargetAppStore](상세의 등록 게이트 판정용)에도 반영한다.
 *
 * 대상 앱 설정은 verification 소관이라 그쪽 domain 계약을 직접 쓴다. 예전에는 core 포트를 경유했는데,
 * 쿨다운·형식 위반을 구분할 수 없었고 중복 제거·최대 개수 제한도 적용되지 않았다.
 * 지금은 [ScreenAppSet] 이 생성 시점에 그 규칙을 강제해 경로를 우회해도 빠지지 않는다.
 */
@HiltViewModel
class ChallengeTargetsViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val targetAppStore: TargetAppStore,
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
                val myApps = runCatching { verificationRepository.getMyScreenApps(challengeId) }.getOrNull() ?: return@launch
                // 익일 적용 대기 세트가 있으면 그쪽을 시드로 쓴다 — 사용자가 마지막으로 고른 것이 그거다.
                val apps = myApps.pending?.apps ?: myApps.apps
                if (apps.isNotEmpty()) {
                    dispatch(ChallengeTargetsReducerEvent.Restored(apps.map { it.packageName }.toSet()))
                }
            }
        }

        private fun save(intent: ChallengeTargetsIntent.Save) {
            if (currentState.isSaving) return
            // 중복 제거·최대 개수 제한은 ScreenAppSet 이 한다. 여기서는 왕복 없이 즉시 알려줄 수 있는
            // 빈 선택만 먼저 막는다.
            if (intent.apps.isEmpty()) {
                emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱을 1개 이상 선택해주세요"))
                return
            }
            dispatch(ChallengeTargetsReducerEvent.Saving)
            viewModelScope.launch {
                runCatching { verificationRepository.updateMyScreenApps(intent.challengeId, ScreenAppSet.of(intent.apps)) }
                    .onSuccess { accepted ->
                        // 상세 화면의 "등록됨" 게이트 판정용 로컬 반영(서버 성공 시에만).
                        // 서버가 접수한 세트를 쓴다.
                        targetAppStore.save(intent.challengeId, accepted.apps.map { it.packageName })
                        // 변경은 항상 익일 00:00 부터 적용된다. "등록됐어요" 로만 끝내면 오늘부터
                        // 측정되는 줄 안다. 이번 저장으로 월 1회를 소진했다는 것도 함께 알린다 —
                        // 모르면 곧바로 다시 바꾸려다 막힌다.
                        emitEffect(ChallengeTargetsEffect.ShowMessage(savedMessage(accepted.nextChangeAvailableAt)))
                        navigationHelper.navigateToBack()
                    }.onFailure { emitEffect(ChallengeTargetsEffect.ShowMessage(it.saveFailureMessage())) }
                dispatch(ChallengeTargetsReducerEvent.Finished)
            }
        }

        /** 저장 성공 안내. 적용 시점(익일)과 다음 변경 가능 시점을 같이 말한다. */
        private fun savedMessage(nextChangeAvailableAt: String?): String {
            val next = monthDayLabel(nextChangeAvailableAt)
            return if (next == null) {
                "대상 앱이 등록됐어요. 내일부터 적용돼요"
            } else {
                "대상 앱이 등록됐어요. 내일부터 적용되고, 다음 변경은 ${next}부터예요"
            }
        }

        /**
         * 저장 실패 안내. 사용자가 할 일이 다른 실패만 갈라 말한다 — 월 1회 소진은 **잠시 후 다시
         * 시도해도 안 되고** 다음 달까지 기다려야 하는데, 기본 문구로 뭉개면 계속 눌러 보게 된다.
         */
        private fun Throwable.saveFailureMessage(): String =
            when (this) {
                // 429 본문의 nextChangeAvailableAt 은 공통 에러 형식에 실을 자리가 없다 —
                // 정확한 날짜는 다음 조회에서 드러나므로 여기서는 사유만 분명히 말한다.
                is SettingChangeLimitException -> message ?: "이번 달 변경 횟수를 모두 썼어요"
                is InvalidScreenAppException -> message ?: DEFAULT_SAVE_FAILURE
                else -> DEFAULT_SAVE_FAILURE
            }

        /** ISO 시각 → "9월 1일". 못 읽으면 null — 날짜를 지어내지 않는다. */
        private fun monthDayLabel(iso: String?): String? {
            val parts = iso?.substringBefore('T')?.split('-')?.takeIf { it.size == 3 } ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null
            return "${month}월 ${day}일"
        }

        private companion object {
            const val DEFAULT_SAVE_FAILURE = "대상 앱 저장에 실패했어요. 잠시 후 다시 시도해주세요"
        }
    }
