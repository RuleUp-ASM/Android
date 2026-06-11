package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.RepeatDay
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.ui.mvi.MviIntent

sealed interface CreateChallengeIntent : MviIntent {
    // 01 · 입력
    data class SetTitle(
        val title: String,
    ) : CreateChallengeIntent

    data class SetDescription(
        val description: String,
    ) : CreateChallengeIntent

    /** AI 추천 요청. 확인 화면의 "다시 추천" 도 같은 intent 를 쓴다. */
    data object Recommend : CreateChallengeIntent

    // 02 · 추천 확인(수정)
    data class SetCoverImage(
        val uri: String?,
    ) : CreateChallengeIntent

    data class SetParticipationType(
        val type: ParticipationType,
    ) : CreateChallengeIntent

    data class SetMinMannerTemperature(
        val temperature: Int,
    ) : CreateChallengeIntent

    data class ToggleRepeatDay(
        val day: RepeatDay,
    ) : CreateChallengeIntent

    data class SetPeriod(
        val startDate: String,
        val durationDays: Int,
    ) : CreateChallengeIntent

    data class ToggleVerificationMethod(
        val method: VerificationMethod,
    ) : CreateChallengeIntent

    data class SetSnsShareEnabled(
        val enabled: Boolean,
    ) : CreateChallengeIntent

    data class SetSnsPhone(
        val phone: String,
    ) : CreateChallengeIntent

    data class SetGroupShare(
        val enabled: Boolean,
    ) : CreateChallengeIntent

    /** 이대로 만들기. */
    data object Create : CreateChallengeIntent
}
