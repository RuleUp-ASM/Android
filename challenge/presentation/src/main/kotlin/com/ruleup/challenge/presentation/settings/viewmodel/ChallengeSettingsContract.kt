package com.ruleup.challenge.presentation.settings.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeField
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.mvi.MviEffect
import com.ruleup.ui.mvi.MviIntent
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.ui.mvi.UiState

sealed interface ChallengeSettingsIntent : MviIntent {
    /** 화면 진입 */
    data class Load(
        val challengeId: String,
    ) : ChallengeSettingsIntent

    data class SetTitle(
        val title: String,
    ) : ChallengeSettingsIntent

    data class SetDescription(
        val description: String,
    ) : ChallengeSettingsIntent

    data class SetCoverImage(
        val uri: String?,
    ) : ChallengeSettingsIntent

    /** 대표 이미지를 기본 이미지로 되돌린다 */
    data object RemoveCoverImage : ChallengeSettingsIntent

    // null 이면 무제한
    data class SetCapacity(
        val capacity: Int?,
    ) : ChallengeSettingsIntent

    data class SetVisibility(
        val visibility: ChallengeVisibility,
    ) : ChallengeSettingsIntent

    data class SetRankingVisible(
        val visible: Boolean,
    ) : ChallengeSettingsIntent

    data class SetMinTier(
        val tier: Tier,
    ) : ChallengeSettingsIntent

    data class SetPeriod(
        val start: String,
        val end: String,
    ) : ChallengeSettingsIntent

    /** 주간 수행 횟수 1~7. */
    data class SetWeeklyCount(
        val count: Int,
    ) : ChallengeSettingsIntent

    data class EditParam(
        val key: String,
        val value: String,
    ) : ChallengeSettingsIntent

    /** 인증 방식 */
    data class SetVerificationType(
        val type: VerificationType,
    ) : ChallengeSettingsIntent

    data class SetWatcherPenalty(
        val enabled: Boolean,
    ) : ChallengeSettingsIntent

    data object Save : ChallengeSettingsIntent

    data object Back : ChallengeSettingsIntent
}

sealed interface ChallengeSettingsEffect : MviEffect {
    data class ShowMessage(
        val message: String,
    ) : ChallengeSettingsEffect
}

/** 챌린지 수정 화면 상태. */
data class ChallengeSettingsState(
    val challengeId: String,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val loaded: ChallengeSettings?,
    val errorMessage: String?,
    // 편집본
    val title: String,
    val description: String,
    val imageUrl: String?,
    // 새로 고른 로컬 이미지.
    val coverImageUri: String?,
    // 기본 이미지로 되돌리기를 눌렀는지
    val removeImage: Boolean,
    // null 이면 무제한
    val capacity: Int?,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    val minTier: Tier?,
    val period: ChallengePeriod,
    val weeklyCount: Int,
    val params: List<ParamSpec>,
    val verificationType: VerificationType?,
    val watcherPenalty: Boolean,
    // 반복 거부로 1시간 수정 잠금이 걸렸을 때 남은 초.
    val moderationLockedSeconds: Int?,
    // 현재 참여 인원.
    val participantCount: Int?,
) : UiState {
    val moderation: ChallengeModeration?
        get() = loaded?.moderation

    /** 정원 하한. */
    val capacityFloor: Int
        get() = participantCount ?: 1

    /** 고를 수 있는 정원 단계. */
    val capacitySteps: List<Int?>
        get() =
            (ChallengeLimits.CREATE_CAPACITY_STEPS.filter { it == null || it >= capacityFloor } + capacity)
                .distinct()
                .sortedWith(nullsLast(naturalOrder()))

    fun editable(field: ChallengeField): Boolean = loaded?.editableFields?.contains(field) == true

    /** 자동 인증으로 되돌릴 수 있는지 */
    val canUseAuto: Boolean
        get() =
            loaded
                ?.config
                ?.verification
                ?.type
                ?.isAuto == true

    /** 저장할 게 있는지. */
    val hasChanges: Boolean
        get() {
            val origin = loaded?.config ?: return false
            return title != origin.title ||
                description != origin.description ||
                removeImage ||
                coverImageUri != null ||
                capacity != origin.capacity ||
                visibility != origin.visibility ||
                rankingVisible != origin.rankingVisible ||
                minTier != origin.minTier ||
                period != origin.period ||
                weeklyCount != origin.weeklyCount ||
                params != origin.params ||
                verificationType != origin.verification.type ||
                watcherPenalty != origin.penalties.watcher
        }

    companion object {
        val initial =
            ChallengeSettingsState(
                challengeId = "",
                isLoading = true,
                isSaving = false,
                loaded = null,
                errorMessage = null,
                title = "",
                description = "",
                imageUrl = null,
                coverImageUri = null,
                removeImage = false,
                capacity = null,
                visibility = null,
                rankingVisible = null,
                minTier = null,
                period = ChallengePeriod(start = "", end = ""),
                // 기본 7회 = 매일
                weeklyCount = ChallengeLimits.WEEKLY_COUNT_MAX,
                params = emptyList(),
                verificationType = null,
                watcherPenalty = false,
                moderationLockedSeconds = null,
                participantCount = null,
            )
    }
}

sealed interface ChallengeSettingsReducerEvent : ReducerEvent {
    data class Loading(
        val challengeId: String,
    ) : ChallengeSettingsReducerEvent

    /** 설정 수신 */
    data class Loaded(
        val settings: ChallengeSettings,
        // 정원 하한 계산용.
        val participantCount: Int?,
        val preserveEdits: Boolean = false,
    ) : ChallengeSettingsReducerEvent

    data class Failed(
        val message: String,
    ) : ChallengeSettingsReducerEvent

    data class Saving(
        val saving: Boolean,
    ) : ChallengeSettingsReducerEvent

    data class WeeklyCountChanged(
        val count: Int,
    ) : ChallengeSettingsReducerEvent

    data class ModerationLocked(
        val retryAfterSeconds: Int?,
    ) : ChallengeSettingsReducerEvent

    data class TitleEntered(
        val title: String,
    ) : ChallengeSettingsReducerEvent

    data class DescriptionEntered(
        val description: String,
    ) : ChallengeSettingsReducerEvent

    data class CoverImageSelected(
        val uri: String?,
    ) : ChallengeSettingsReducerEvent

    data object CoverImageRemoved : ChallengeSettingsReducerEvent

    data class CapacityChanged(
        val capacity: Int?,
    ) : ChallengeSettingsReducerEvent

    data class VisibilitySelected(
        val visibility: ChallengeVisibility,
    ) : ChallengeSettingsReducerEvent

    data class RankingVisibleChanged(
        val visible: Boolean,
    ) : ChallengeSettingsReducerEvent

    data class MinTierChanged(
        val tier: Tier,
    ) : ChallengeSettingsReducerEvent

    data class PeriodChanged(
        val start: String,
        val end: String,
    ) : ChallengeSettingsReducerEvent

    data class ParamEdited(
        val key: String,
        val value: String,
    ) : ChallengeSettingsReducerEvent

    data class VerificationTypeSelected(
        val type: VerificationType,
    ) : ChallengeSettingsReducerEvent

    data class WatcherPenaltyChanged(
        val enabled: Boolean,
    ) : ChallengeSettingsReducerEvent
}
