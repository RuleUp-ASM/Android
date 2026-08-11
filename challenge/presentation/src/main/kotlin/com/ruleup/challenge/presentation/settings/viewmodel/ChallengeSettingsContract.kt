package com.ruleup.challenge.presentation.settings.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeField
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
    /** 화면 진입 — 현재 설정·editableFields·version 을 불러온다. */
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

    /** 대표 이미지를 기본 이미지로 되돌린다 — PATCH 에서 유일하게 명시적 null 을 보내는 경로다. */
    data object RemoveCoverImage : ChallengeSettingsIntent

    data class SetCapacity(
        val capacity: Int,
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

    /** 주간 수행 횟수 1~7. 요일이 아니라 "그 주에 몇 번" 이다. */
    data class SetWeeklyCount(
        val count: Int,
    ) : ChallengeSettingsIntent

    data class EditParam(
        val key: String,
        val value: String,
    ) : ChallengeSettingsIntent

    /** 인증 방식 — AUTO → MANUAL 단방향만 허용된다. */
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

/**
 * 챌린지 수정 화면 상태.
 *
 * 원본([loaded])을 불변으로 들고 편집본을 따로 둔다 — **바뀐 필드만 PATCH 로 보내야 하므로**
 * 무엇이 달라졌는지 비교할 기준이 필요하다.
 *
 * 잠금은 [ChallengeSettings.editableFields] 를 그대로 따른다. 클라이언트가 규칙을 재구현하면
 * 서버와 어긋나는 순간 409 를 받고서야 알게 된다.
 */
data class ChallengeSettingsState(
    val challengeId: String,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val loaded: ChallengeSettings?,
    val errorMessage: String?,
    // ---- 편집본 ----
    val title: String,
    val description: String,
    val imageUrl: String?,
    // 새로 고른 로컬 이미지. 저장 시 업로드해 URL 로 바꾼다.
    val coverImageUri: String?,
    // 기본 이미지로 되돌리기를 눌렀는지 — imageUrl 에 명시적 null 을 보낼지 가른다.
    val removeImage: Boolean,
    val capacity: Int,
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
    // 현재 참여 인원. settings 응답에 없어 공개 상세에서 함께 받아 온다.
    val participantCount: Int?,
) : UiState {
    val moderation: ChallengeModeration?
        get() = loaded?.moderation

    /** 정원 하한. 현재 참여 인원보다 작게 줄일 수 없다(서버도 `CAPACITY_BELOW_CURRENT` 로 막는다). */
    val capacityFloor: Int
        get() = participantCount ?: 1

    fun editable(field: ChallengeField): Boolean = loaded?.editableFields?.contains(field) == true

    /** 자동 인증으로 되돌릴 수 있는지 — 원본이 AUTO 였을 때만 가능하다(단방향 전환). */
    val canUseAuto: Boolean
        get() = loaded?.config?.verification?.type == VerificationType.AUTO

    /** 저장할 게 있는지. 아무것도 안 바꿨으면 버튼을 열지 않는다. */
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
        // 명세: 주간 횟수 1~7 · 기본 7(매일)
        const val WEEKLY_COUNT_MIN = 1
        const val WEEKLY_COUNT_MAX = 7

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
                capacity = 0,
                visibility = null,
                rankingVisible = null,
                minTier = null,
                period = ChallengePeriod(start = "", end = ""),
                weeklyCount = WEEKLY_COUNT_MAX,
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

    /** 설정 수신 — 편집본을 원본값으로 초기화한다. 409 후 재조회도 같은 경로를 탄다. */
    data class Loaded(
        val settings: ChallengeSettings,
        // 정원 하한 계산용. 공개 상세에서 함께 받으며 실패하면 null(하한만 못 잠근다).
        val participantCount: Int?,
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
        val capacity: Int,
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
