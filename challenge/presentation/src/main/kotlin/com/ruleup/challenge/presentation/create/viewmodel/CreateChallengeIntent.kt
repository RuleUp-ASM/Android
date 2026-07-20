package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ParamValue
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.RepeatDay
import com.ruleup.challenge.domain.entity.SelectedMethod
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

    data class SetMaxParticipants(
        val count: Int,
    ) : CreateChallengeIntent

    data class ToggleRepeatDay(
        val day: RepeatDay,
    ) : CreateChallengeIntent

    data class SetPeriod(
        val startDate: String,
        val durationDays: Int,
    ) : CreateChallengeIntent

    /** AUTO/MANUAL 인증 방식 선택. */
    data class SelectMethod(
        val method: SelectedMethod,
    ) : CreateChallengeIntent

    /** 목표값 편집. */
    data class EditParam(
        val key: String,
        val value: ParamValue,
    ) : CreateChallengeIntent

    /** 권한 요청 결과(화면이 OS 다이얼로그를 띄운 뒤 허용된 토큰을 돌려준다). */
    data class PermissionsResult(
        val granted: Set<String>,
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
