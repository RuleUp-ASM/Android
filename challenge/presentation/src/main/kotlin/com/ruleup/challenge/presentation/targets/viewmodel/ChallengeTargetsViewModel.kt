package com.ruleup.challenge.presentation.targets.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.repository.TargetAppStore
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.mvi.MviViewModel
import com.ruleup.verification.domain.entity.InvalidScreenAppException
import com.ruleup.verification.domain.entity.ScreenAppSet
import com.ruleup.verification.domain.entity.SettingChangeLimitException
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.repository.UsageTargetStore
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 대상 앱 등록 ViewModel. */
@HiltViewModel
class ChallengeTargetsViewModel
    @Inject
    constructor(
        private val verificationRepository: VerificationRepository,
        private val targetAppStore: TargetAppStore,
        private val usageTargetStore: UsageTargetStore,
        private val syncScheduler: SyncScheduler,
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
                ChallengeTargetsReducerEvent.Loading -> state.copy(isLoading = true, loadFailed = false)
                ChallengeTargetsReducerEvent.LoadFailed -> state.copy(isLoading = false, loadFailed = true)
                is ChallengeTargetsReducerEvent.Restored -> state.copy(isLoading = false, restoredPackages = event.packages)
                ChallengeTargetsReducerEvent.Saving -> state.copy(isSaving = true)
                ChallengeTargetsReducerEvent.Finished -> state.copy(isSaving = false)
            }

        private fun load(challengeId: String) {
            viewModelScope.launch {
                dispatch(ChallengeTargetsReducerEvent.Loading)
                val myApps =
                    try {
                        verificationRepository.getMyScreenApps(challengeId)
                    } catch (
                        cancelled: kotlinx.coroutines.CancellationException,
                    ) {
                        throw cancelled
                    } catch (_: Exception) {
                        dispatch(ChallengeTargetsReducerEvent.LoadFailed)
                        return@launch
                    }
                // 익일 적용 대기 세트가 있으면 그쪽을 시드로 쓴다
                val apps = myApps?.pending?.apps ?: myApps?.apps.orEmpty()
                dispatch(ChallengeTargetsReducerEvent.Restored(apps.map { it.packageName }.toSet()))
            }
        }

        private fun save(intent: ChallengeTargetsIntent.Save) {
            if (currentState.isSaving || currentState.loadFailed) return
            // 중복 제거·최대 개수 제한은 ScreenAppSet 이 한다.
            if (intent.apps.isEmpty()) {
                emitEffect(ChallengeTargetsEffect.ShowMessage("대상 앱을 1개 이상 선택해주세요"))
                return
            }
            dispatch(ChallengeTargetsReducerEvent.Saving)
            viewModelScope.launch {
                runCatching { verificationRepository.updateMyScreenApps(intent.challengeId, ScreenAppSet.of(intent.apps)) }
                    .onSuccess { accepted ->
                        // 상세 화면의 "등록됨" 게이트 판정용 로컬 반영(서버 성공 시에만).
                        val packages = accepted.apps.map { it.packageName }
                        targetAppStore.save(intent.challengeId, packages)
                        // 수집기가 읽는 저장소는 이쪽이다.
                        runCatching { verificationRepository.getMyScreenApps(intent.challengeId) }.getOrNull()?.let { current ->
                            usageTargetStore.replaceFor(intent.challengeId, current.apps.mapTo(linkedSetOf()) { it.packageName })
                        }
                        // 바뀐 대상을 다음 주기까지 기다리지 않고 한 번 흘려보낸다
                        syncScheduler.enqueueCatchUp()
                        // 변경은 항상 익일 00:00 부터 적용된다.
                        emitEffect(ChallengeTargetsEffect.ShowMessage(savedMessage(accepted.nextChangeAvailableAt)))
                        navigationHelper.navigateToBack()
                    }.onFailure { emitEffect(ChallengeTargetsEffect.ShowMessage(it.saveFailureMessage())) }
                dispatch(ChallengeTargetsReducerEvent.Finished)
            }
        }

        /** 저장 성공 안내. */
        private fun savedMessage(nextChangeAvailableAt: String?): String {
            val next = monthDayLabel(nextChangeAvailableAt)
            return if (next == null) {
                "대상 앱이 등록됐어요. 내일부터 적용돼요"
            } else {
                "대상 앱이 등록됐어요. 내일부터 적용되고, 다음 변경은 ${next}부터예요"
            }
        }

        /** 저장 실패 안내. */
        private fun Throwable.saveFailureMessage(): String =
            when (this) {
                // 429 본문의 nextChangeAvailableAt 은 공통 에러 형식에 실을 자리가 없다
                is SettingChangeLimitException -> message ?: "이번 달 변경 횟수를 모두 썼어요"
                is InvalidScreenAppException -> message ?: DEFAULT_SAVE_FAILURE
                else -> DEFAULT_SAVE_FAILURE
            }

        /** ISO 시각 → "9월 1일". */
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
