package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.mvi.ReducerEvent

sealed interface CreateChallengeReducerEvent : ReducerEvent {
    // ---- 입력 화면 ----
    data class RoutineDescriptionEntered(
        val description: String,
    ) : CreateChallengeReducerEvent

    data object TemplatesLoading : CreateChallengeReducerEvent

    data class TemplatesLoaded(
        val templates: List<RoutineTemplate>,
    ) : CreateChallengeReducerEvent

    data object TemplatesFailed : CreateChallengeReducerEvent

    data object Drafting : CreateChallengeReducerEvent

    data object DraftFailed : CreateChallengeReducerEvent

    /** 폴백 — 입력을 지우지 않고 안내만 띄운다. */
    data class DraftFellBack(
        val message: String,
    ) : CreateChallengeReducerEvent

    /** 429 — 버튼을 잠그고 카운트다운을 표시한다. */
    data class DraftRateLimited(
        val retryAfterSeconds: Int?,
    ) : CreateChallengeReducerEvent

    data object RateLimitCleared : CreateChallengeReducerEvent

    /** 초안 수신 — 편집본을 초안값으로 채우고 idempotency key 를 1회 발급한다. */
    data class DraftReceived(
        val draft: DraftResult.Ok,
        val idempotencyKey: String,
    ) : CreateChallengeReducerEvent

    // ---- 확인 화면 ----
    data class TitleEntered(
        val title: String,
    ) : CreateChallengeReducerEvent

    data class DescriptionEntered(
        val description: String,
    ) : CreateChallengeReducerEvent

    data class CoverImageSelected(
        val uri: String?,
    ) : CreateChallengeReducerEvent

    data class ModeSelected(
        val mode: ChallengeMode,
    ) : CreateChallengeReducerEvent

    data class VisibilitySelected(
        val visibility: ChallengeVisibility,
    ) : CreateChallengeReducerEvent

    data class RankingVisibleChanged(
        val visible: Boolean,
    ) : CreateChallengeReducerEvent

    data class CapacityChanged(
        val capacity: Int,
    ) : CreateChallengeReducerEvent

    data class MinTierChanged(
        val tier: Tier,
    ) : CreateChallengeReducerEvent

    data class PeriodChanged(
        val start: String,
        val end: String,
    ) : CreateChallengeReducerEvent

    data class ParamEdited(
        val key: String,
        val value: String,
    ) : CreateChallengeReducerEvent

    data class VerificationTypeSelected(
        val type: VerificationType,
    ) : CreateChallengeReducerEvent

    data class WatcherPenaltyChanged(
        val enabled: Boolean,
    ) : CreateChallengeReducerEvent

    data class PermissionsGranted(
        val tokens: Set<String>,
    ) : CreateChallengeReducerEvent

    data object Creating : CreateChallengeReducerEvent

    data object CreateFailed : CreateChallengeReducerEvent

    /** 생성 성공 — 권한 요청이 남았을 때만 화면에 머문다. */
    data class Created(
        val challengeId: String,
    ) : CreateChallengeReducerEvent
}
