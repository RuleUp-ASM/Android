package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.RepeatDay
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.ui.mvi.ReducerEvent

sealed interface CreateChallengeReducerEvent : ReducerEvent {
    data class TitleEntered(
        val title: String,
    ) : CreateChallengeReducerEvent

    data class DescriptionEntered(
        val description: String,
    ) : CreateChallengeReducerEvent

    data object Recommending : CreateChallengeReducerEvent

    data object RecommendFailed : CreateChallengeReducerEvent

    data class RecommendationReceived(
        val recommendation: ChallengeRecommendation,
    ) : CreateChallengeReducerEvent

    data class CoverImageSelected(
        val uri: String?,
    ) : CreateChallengeReducerEvent

    data class ParticipationTypeSelected(
        val type: ParticipationType,
    ) : CreateChallengeReducerEvent

    data class MinMannerChanged(
        val temperature: Int,
    ) : CreateChallengeReducerEvent

    data class RepeatDayToggled(
        val day: RepeatDay,
    ) : CreateChallengeReducerEvent

    data class PeriodChanged(
        val startDate: String,
        val durationDays: Int,
    ) : CreateChallengeReducerEvent

    data class VerificationToggled(
        val method: VerificationMethod,
    ) : CreateChallengeReducerEvent

    data class SnsShareChanged(
        val enabled: Boolean,
    ) : CreateChallengeReducerEvent

    data class SnsPhoneEntered(
        val phone: String,
    ) : CreateChallengeReducerEvent

    data class GroupShareChanged(
        val enabled: Boolean,
    ) : CreateChallengeReducerEvent

    data object Creating : CreateChallengeReducerEvent

    data object CreateFailed : CreateChallengeReducerEvent
}
