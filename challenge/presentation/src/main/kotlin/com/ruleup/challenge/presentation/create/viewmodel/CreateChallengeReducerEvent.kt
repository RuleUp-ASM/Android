package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.mvi.ReducerEvent
import com.ruleup.verification.domain.entity.VerificationAccess

sealed interface CreateChallengeReducerEvent : ReducerEvent {
    /** 플로우를 나가 처음 상태로 되돌린다. */
    data object Reset : CreateChallengeReducerEvent

    // 입력 화면
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

    data object DraftExpired : CreateChallengeReducerEvent

    /** 폴백 */
    data class DraftFellBack(
        val message: String,
    ) : CreateChallengeReducerEvent

    /** 429 */
    data class DraftRateLimited(
        val retryAfterSeconds: Int?,
    ) : CreateChallengeReducerEvent

    /** 카운트다운 1초 경과. */
    data object RateLimitTicked : CreateChallengeReducerEvent

    data object RateLimitCleared : CreateChallengeReducerEvent

    /** 폴백 화면을 벗어났다. */
    data object FallbackDismissed : CreateChallengeReducerEvent

    /** 초안 수신 */
    data class DraftReceived(
        val draft: DraftResult.Ok,
        val idempotencyKey: String,
    ) : CreateChallengeReducerEvent

    // 확인 화면
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
        val capacity: Int?,
    ) : CreateChallengeReducerEvent

    data class MinTierChanged(
        val tier: Tier,
    ) : CreateChallengeReducerEvent

    data class PeriodChanged(
        val start: String,
        val end: String,
    ) : CreateChallengeReducerEvent

    data class WeeklyCountChanged(
        val count: Int,
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

    data class VerificationAccessSubmitting(
        val submitting: Boolean,
    ) : CreateChallengeReducerEvent

    data object Creating : CreateChallengeReducerEvent

    data object CreateFailed : CreateChallengeReducerEvent

    data class VerificationAccessRequested(
        val access: VerificationAccess?,
    ) : CreateChallengeReducerEvent

    /** 생성 성공 */
    data class Created(
        val challengeId: String,
    ) : CreateChallengeReducerEvent
}
