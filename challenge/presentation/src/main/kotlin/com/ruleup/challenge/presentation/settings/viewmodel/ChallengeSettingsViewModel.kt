package com.ruleup.challenge.presentation.settings.viewmodel

import androidx.lifecycle.viewModelScope
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeNotEditableException
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeVersionConflictException
import com.ruleup.challenge.domain.entity.ModerationLockedException
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.toEntries
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.domain.helper.NavigationHelper
import com.ruleup.ui.error.userFacingMessage
import com.ruleup.ui.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 챌린지 수정 ViewModel (방장 전용). */
@HiltViewModel
class ChallengeSettingsViewModel
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
        private val navigationHelper: NavigationHelper,
    ) : MviViewModel<
            ChallengeSettingsIntent,
            ChallengeSettingsState,
            ChallengeSettingsReducerEvent,
            ChallengeSettingsEffect,
        >(
            ChallengeSettingsState.initial,
        ) {
        override fun onIntent(intent: ChallengeSettingsIntent) {
            when (intent) {
                is ChallengeSettingsIntent.Load -> load(intent.challengeId)

                is ChallengeSettingsIntent.SetTitle ->
                    dispatch(ChallengeSettingsReducerEvent.TitleEntered(intent.title))

                is ChallengeSettingsIntent.SetDescription ->
                    dispatch(ChallengeSettingsReducerEvent.DescriptionEntered(intent.description))

                is ChallengeSettingsIntent.SetCoverImage ->
                    dispatch(ChallengeSettingsReducerEvent.CoverImageSelected(intent.uri))

                ChallengeSettingsIntent.RemoveCoverImage ->
                    dispatch(ChallengeSettingsReducerEvent.CoverImageRemoved)

                is ChallengeSettingsIntent.SetCapacity -> setCapacity(intent.capacity)

                is ChallengeSettingsIntent.SetVisibility ->
                    dispatch(ChallengeSettingsReducerEvent.VisibilitySelected(intent.visibility))

                is ChallengeSettingsIntent.SetRankingVisible ->
                    dispatch(ChallengeSettingsReducerEvent.RankingVisibleChanged(intent.visible))

                is ChallengeSettingsIntent.SetMinTier ->
                    dispatch(ChallengeSettingsReducerEvent.MinTierChanged(intent.tier))

                is ChallengeSettingsIntent.SetPeriod ->
                    dispatch(ChallengeSettingsReducerEvent.PeriodChanged(intent.start, intent.end))

                is ChallengeSettingsIntent.SetWeeklyCount ->
                    dispatch(ChallengeSettingsReducerEvent.WeeklyCountChanged(intent.count))

                is ChallengeSettingsIntent.EditParam ->
                    dispatch(ChallengeSettingsReducerEvent.ParamEdited(intent.key, intent.value))

                is ChallengeSettingsIntent.SetVerificationType -> setVerificationType(intent.type)

                is ChallengeSettingsIntent.SetWatcherPenalty ->
                    dispatch(ChallengeSettingsReducerEvent.WatcherPenaltyChanged(intent.enabled))

                ChallengeSettingsIntent.Save -> save()

                ChallengeSettingsIntent.Back -> navigationHelper.navigateToBack()
            }
        }

        override fun reduce(
            state: ChallengeSettingsState,
            event: ChallengeSettingsReducerEvent,
        ): ChallengeSettingsState =
            when (event) {
                is ChallengeSettingsReducerEvent.Loading ->
                    state.copy(challengeId = event.challengeId, isLoading = true, errorMessage = null)

                is ChallengeSettingsReducerEvent.Loaded -> state.applyLoaded(event.settings, event.participantCount, event.preserveEdits)

                is ChallengeSettingsReducerEvent.Failed ->
                    state.copy(isLoading = false, isSaving = false, errorMessage = event.message)

                is ChallengeSettingsReducerEvent.Saving -> state.copy(isSaving = event.saving)

                is ChallengeSettingsReducerEvent.ModerationLocked ->
                    state.copy(isSaving = false, moderationLockedSeconds = event.retryAfterSeconds)

                is ChallengeSettingsReducerEvent.TitleEntered -> state.copy(title = event.title)

                is ChallengeSettingsReducerEvent.DescriptionEntered -> state.copy(description = event.description)

                is ChallengeSettingsReducerEvent.CoverImageSelected ->
                    // 새 사진을 고르면 "기본으로 되돌리기"는 취소된 것으로 본다.
                    state.copy(coverImageUri = event.uri, removeImage = false)

                ChallengeSettingsReducerEvent.CoverImageRemoved ->
                    state.copy(coverImageUri = null, removeImage = true)

                is ChallengeSettingsReducerEvent.CapacityChanged -> state.copy(capacity = event.capacity)

                is ChallengeSettingsReducerEvent.VisibilitySelected -> state.copy(visibility = event.visibility)

                is ChallengeSettingsReducerEvent.RankingVisibleChanged -> state.copy(rankingVisible = event.visible)

                is ChallengeSettingsReducerEvent.MinTierChanged -> state.copy(minTier = event.tier)

                is ChallengeSettingsReducerEvent.PeriodChanged ->
                    state.copy(period = state.period.copy(start = event.start, end = event.end))

                is ChallengeSettingsReducerEvent.WeeklyCountChanged ->
                    // 범위 밖 값은 서버가 400 INVALID_WEEKLY_COUNT 로 막는다.
                    state.copy(
                        weeklyCount =
                            event.count.coerceIn(
                                ChallengeLimits.WEEKLY_COUNT_MIN,
                                ChallengeLimits.WEEKLY_COUNT_MAX,
                            ),
                    )

                is ChallengeSettingsReducerEvent.ParamEdited ->
                    state.copy(
                        params = state.params.map { if (it.key == event.key) it.copy(value = event.value) else it },
                    )

                is ChallengeSettingsReducerEvent.VerificationTypeSelected ->
                    state.copy(verificationType = event.type)

                is ChallengeSettingsReducerEvent.WatcherPenaltyChanged -> state.copy(watcherPenalty = event.enabled)
            }

        private fun ChallengeSettingsState.applyLoaded(
            settings: ChallengeSettings,
            participantCount: Int?,
            preserveEdits: Boolean = false,
        ): ChallengeSettingsState {
            val config = settings.config
            val previous = loaded?.config

            fun <T> retain(
                field: com.ruleup.challenge.domain.entity.ChallengeField,
                edited: T,
                old: T?,
                latest: T,
            ): T = if (preserveEdits && field in settings.editableFields && edited != old) edited else latest
            return copy(
                isLoading = false,
                isSaving = false,
                loaded = settings,
                errorMessage = null,
                title = retain(com.ruleup.challenge.domain.entity.ChallengeField.TITLE, title, previous?.title, config.title),
                description =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.DESCRIPTION,
                        description,
                        previous?.description,
                        config.description,
                    ),
                imageUrl = config.imageUrl,
                coverImageUri =
                    if (preserveEdits &&
                        com.ruleup.challenge.domain.entity.ChallengeField.IMAGE_URL in settings.editableFields
                    ) {
                        coverImageUri
                    } else {
                        null
                    },
                removeImage =
                    preserveEdits && com.ruleup.challenge.domain.entity.ChallengeField.IMAGE_URL in settings.editableFields && removeImage,
                capacity =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.CAPACITY,
                        capacity,
                        previous?.capacity,
                        config.capacity,
                    ),
                visibility =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.VISIBILITY,
                        visibility,
                        previous?.visibility,
                        config.visibility,
                    ),
                rankingVisible =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.RANKING_VISIBLE,
                        rankingVisible,
                        previous?.rankingVisible,
                        config.rankingVisible,
                    ),
                minTier =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.MIN_TIER,
                        minTier,
                        previous?.minTier,
                        config.minTier,
                    ),
                period = retain(com.ruleup.challenge.domain.entity.ChallengeField.PERIOD, period, previous?.period, config.period),
                weeklyCount =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.WEEKLY_COUNT,
                        weeklyCount,
                        previous?.weeklyCount,
                        config.weeklyCount,
                    ),
                params = retain(com.ruleup.challenge.domain.entity.ChallengeField.PARAMS, params, previous?.params, config.params),
                verificationType =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.VERIFICATION,
                        verificationType,
                        previous?.verification?.type,
                        config.verification.type,
                    ),
                watcherPenalty =
                    retain(
                        com.ruleup.challenge.domain.entity.ChallengeField.PENALTIES,
                        watcherPenalty,
                        previous?.penalties?.watcher,
                        config.penalties.watcher,
                    ),
                participantCount = participantCount,
            )
        }

        private fun load(
            challengeId: String,
            preserveEdits: Boolean = false,
        ) {
            viewModelScope.launch {
                dispatch(ChallengeSettingsReducerEvent.Loading(challengeId))
                runCatching { challengeRepository.getSettings(challengeId) }
                    .onSuccess { settings ->
                        // 정원 하한 계산에 현재 인원이 필요한데 settings 응답에 없다.
                        val participants =
                            runCatching { challengeRepository.getChallenge(challengeId).participantCount }.getOrNull()
                        dispatch(ChallengeSettingsReducerEvent.Loaded(settings, participants, preserveEdits))
                    }.onFailure {
                        dispatch(ChallengeSettingsReducerEvent.Failed(it.userFacingMessage("설정을 불러오지 못했어요")))
                    }
            }
        }

        private fun setCapacity(capacity: Int?) {
            // 현재 인원보다 작은 정원은 받지 않는다
            if (capacity != null && capacity < currentState.capacityFloor) return
            dispatch(ChallengeSettingsReducerEvent.CapacityChanged(capacity))
        }

        /** AUTO → MANUAL 단방향. */
        private fun setVerificationType(type: VerificationType) {
            val state = currentState
            if (type.isAuto && state.verificationType?.isAuto == false) {
                emitEffect(ChallengeSettingsEffect.ShowMessage("직접 체크로 바꾼 뒤에는 되돌릴 수 없어요"))
                return
            }
            if (type.isAuto && !state.canUseAuto) {
                emitEffect(ChallengeSettingsEffect.ShowMessage("이 루틴은 자동 인증을 쓸 수 없어요"))
                return
            }
            dispatch(ChallengeSettingsReducerEvent.VerificationTypeSelected(type))
        }

        /** 저장. */
        private fun save() {
            val state = currentState
            val origin = state.loaded ?: return
            if (state.isSaving || !state.hasChanges) return

            viewModelScope.launch {
                dispatch(ChallengeSettingsReducerEvent.Saving(true))
                runCatching {
                    // 새 사진을 골랐으면 먼저 업로드해 URL 을 확보한다(서버가 발급 주체를 검증한다).
                    val uploadedUrl = state.coverImageUri?.let { challengeRepository.uploadImage(it) }
                    challengeRepository.update(state.challengeId, state.toUpdate(origin, uploadedUrl))
                }.onSuccess {
                    dispatch(ChallengeSettingsReducerEvent.Saving(false))
                    emitEffect(ChallengeSettingsEffect.ShowMessage("저장했어요"))
                    navigationHelper.navigateToBack()
                }.onFailure { error -> handleSaveFailure(error) }
            }
        }

        private fun handleSaveFailure(error: Throwable) {
            when (error) {
                // 둘 다 "서버 기준으로 다시 그려라"로 귀결된다
                is ChallengeVersionConflictException, is ChallengeNotEditableException -> {
                    dispatch(ChallengeSettingsReducerEvent.Saving(false))
                    emitEffect(ChallengeSettingsEffect.ShowMessage(error.message.orEmpty()))
                    load(currentState.challengeId, preserveEdits = true)
                }

                is ModerationLockedException ->
                    dispatch(ChallengeSettingsReducerEvent.ModerationLocked(error.retryAfterSeconds))

                else -> {
                    dispatch(ChallengeSettingsReducerEvent.Saving(false))
                    emitEffect(ChallengeSettingsEffect.ShowMessage(error.userFacingMessage("저장하지 못했어요")))
                }
            }
        }

        /** 원본과 다른 필드만 담는다. */
        private fun ChallengeSettingsState.toUpdate(
            origin: ChallengeSettings,
            uploadedUrl: String?,
        ): ChallengeUpdate {
            val config = origin.config
            return ChallengeUpdate(
                version = origin.version,
                title = title.takeIf { it != config.title },
                description = description.takeIf { it != config.description },
                imageUrl = uploadedUrl,
                removeImage = removeImage,
                capacity = capacity.takeIf { it != config.capacity },
                // 무제한으로 바꿀 때만 null 을 명시 전송한다
                unlimitedCapacity = capacity == null && config.capacity != null,
                visibility = visibility.takeIf { it != config.visibility },
                rankingVisible = rankingVisible.takeIf { it != config.rankingVisible },
                minTier = minTier.takeIf { it != config.minTier },
                period = period.takeIf { it != config.period },
                weeklyCount = weeklyCount.takeIf { it != config.weeklyCount },
                params = params.takeIf { it != config.params }?.toEntries(),
                verification =
                    verificationType
                        ?.takeIf { it != config.verification.type }
                        ?.let { config.verification.copy(type = it) },
                watcherPenalty = watcherPenalty.takeIf { it != config.penalties.watcher },
            )
        }
    }
