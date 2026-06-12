package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.RepeatDay
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.entity.user.InterestCategory
import com.ruleup.ui.mvi.UiState

/**
 * 챌린지 생성 플로우 상태. 입력(01)과 AI 추천 확인(02)이 같은 인스턴스를 공유한다.
 * 추천 수신 시 편집 가능 필드가 추천값으로 채워지고, 이후 사용자가 자유롭게 수정한다.
 */
data class CreateChallengeState(
    // 01 · 입력
    val title: String,
    val description: String,
    val isRecommending: Boolean,
    // 02 · 추천 확인 (추천 수신 후 편집 가능)
    val hasRecommendation: Boolean,
    val coverImageUri: String?,
    val category: InterestCategory?,
    val participationType: ParticipationType,
    // 그룹 전용 참여 기준 (37~99℃)
    val minMannerTemperature: Int,
    val repeatDays: List<RepeatDay>,
    // ISO yyyy-MM-dd
    val startDate: String,
    val durationDays: Int,
    val verificationMethods: List<VerificationMethod>,
    // 패널티/보상
    val mannerDeduction: Double,
    val mannerGain: Double,
    val snsShareEnabled: Boolean,
    val snsPhone: String,
    val groupShare: Boolean,
    val isCreating: Boolean,
) : UiState {
    companion object {
        const val TITLE_MAX = 30
        const val DESCRIPTION_MAX = 200
        const val MANNER_MIN = 37
        const val MANNER_MAX = 99

        val initial =
            CreateChallengeState(
                title = "",
                description = "",
                isRecommending = false,
                hasRecommendation = false,
                coverImageUri = null,
                category = null,
                participationType = ParticipationType.SOLO,
                minMannerTemperature = 65,
                repeatDays = emptyList(),
                startDate = "",
                durationDays = 14,
                verificationMethods = emptyList(),
                mannerDeduction = 2.4,
                mannerGain = 0.5,
                snsShareEnabled = false,
                snsPhone = "",
                groupShare = true,
                isCreating = false,
            )
    }
}
