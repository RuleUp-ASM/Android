package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.RepeatDay
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.VerificationOption
import com.ruleup.domain.entity.category.Category
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
    // 루틴 템플릿 매칭 성공 여부 / 매칭된 템플릿 id
    val matched: Boolean,
    val templateId: Int?,
    val coverImageUri: String?,
    val category: Category?,
    val participationType: ParticipationType,
    // 그룹 전용 최대 참여 인원 (SOLO 는 1 로 전송)
    val maxParticipants: Int,
    // 그룹 전용 참여 기준 (MANNER_MIN ~ maxMannerTemperature)
    val minMannerTemperature: Int,
    // 기준 온도 입력 상한 (= 생성자 현재 온도, 추천 응답 maxMannerTemperature). 기본은 MANNER_MAX.
    val maxMannerTemperature: Int,
    val repeatDays: List<RepeatDay>,
    // ISO yyyy-MM-dd
    val startDate: String,
    val durationDays: Int,
    // 인증 (루틴 매칭 기반)
    val options: List<VerificationOption>,
    val selectedMethod: SelectedMethod,
    val params: List<ParamSpec>,
    // AUTO 권한: 누적 허용 토큰 + 한 번 요청했는지(무한 재요청 방지)
    val grantedPermissions: Set<String>,
    val permissionRequested: Boolean,
    val rationale: String?,
    // 패널티/보상
    val mannerDeduction: Double,
    val mannerGain: Double,
    val snsShareEnabled: Boolean,
    val snsPhone: String,
    val groupShare: Boolean,
    val isCreating: Boolean,
) : UiState {
    /** 현재 선택된 인증 방식의 옵션(없으면 null). */
    val selectedOption: VerificationOption?
        get() = options.firstOrNull { it.method == selectedMethod }

    /** AUTO 옵션 존재 여부 (없으면 자동 인증 비활성). */
    val hasAutoOption: Boolean
        get() = options.any { it.method == SelectedMethod.AUTO }

    companion object {
        const val TITLE_MAX = 30
        const val DESCRIPTION_MAX = 200
        const val MANNER_MIN = 37
        const val MANNER_MAX = 99

        // 그룹 최대 인원 범위·기본값 (명세: GROUP 필수, 최소 2)
        const val GROUP_PARTICIPANTS_MIN = 2
        const val GROUP_PARTICIPANTS_MAX = 100
        const val GROUP_MAX_PARTICIPANTS_DEFAULT = 10

        val initial =
            CreateChallengeState(
                title = "",
                description = "",
                isRecommending = false,
                hasRecommendation = false,
                matched = false,
                templateId = null,
                coverImageUri = null,
                category = null,
                participationType = ParticipationType.SOLO,
                maxParticipants = GROUP_MAX_PARTICIPANTS_DEFAULT,
                // 최저값(MANNER_MIN)은 "제한 없음"으로 취급해 서버로 null 을 보낸다(생성 시).
                // 기본을 최저로 두어, 생성자 본인 온도보다 높은 기준이 기본값으로 강제돼 생성이 막히던 문제를 피한다.
                minMannerTemperature = MANNER_MIN,
                maxMannerTemperature = MANNER_MAX,
                repeatDays = emptyList(),
                startDate = "",
                durationDays = 14,
                options = emptyList(),
                selectedMethod = SelectedMethod.MANUAL,
                params = emptyList(),
                grantedPermissions = emptySet(),
                permissionRequested = false,
                rationale = null,
                mannerDeduction = 2.4,
                mannerGain = 0.5,
                snsShareEnabled = false,
                snsPhone = "",
                groupShare = true,
                isCreating = false,
            )
    }
}
