package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.mvi.MviIntent

/** 포커스 아웃 시점에 수정 여부를 판정하는 텍스트 필드. */
enum class TextEditField {
    TITLE,
    DESCRIPTION,
}

sealed interface CreateChallengeIntent : MviIntent {
    // 입력 화면

    /** 화면 진입 */
    data object Load : CreateChallengeIntent

    data object ConfirmOpened : CreateChallengeIntent

    data class SetRoutineDescription(
        val description: String,
    ) : CreateChallengeIntent

    /** 경로 B: 설명으로 초안 생성(LLM). */
    data object SubmitDescription : CreateChallengeIntent

    /** 폴백 화면에서 입력 화면으로 돌아간다. */
    data object DismissFallback : CreateChallengeIntent

    /** 초안 생성 취소(뒤로가기). */
    data object CancelDrafting : CreateChallengeIntent

    /** 경로 A: 추천 칩 탭 → 템플릿 초안(LLM 미경유, 대기 없음). */
    data class SelectTemplate(
        val templateId: Long,
    ) : CreateChallengeIntent

    /** 추천 영역만 재시도. */
    data object RetryTemplates : CreateChallengeIntent

    // 확인 화면
    data class SetTitle(
        val title: String,
    ) : CreateChallengeIntent

    data class SetDescription(
        val description: String,
    ) : CreateChallengeIntent

    data class SetCoverImage(
        val uri: String?,
    ) : CreateChallengeIntent

    data class SetMode(
        val mode: ChallengeMode,
    ) : CreateChallengeIntent

    data class SetVisibility(
        val visibility: ChallengeVisibility,
    ) : CreateChallengeIntent

    data class SetRankingVisible(
        val visible: Boolean,
    ) : CreateChallengeIntent

    data class SetCapacity(
        val capacity: Int?,
    ) : CreateChallengeIntent

    data class SetMinTier(
        val tier: Tier,
    ) : CreateChallengeIntent

    data class SetPeriod(
        val start: String,
        val end: String,
    ) : CreateChallengeIntent

    /** 주간 수행 횟수 1~7. */
    data class SetWeeklyCount(
        val count: Int,
    ) : CreateChallengeIntent

    /** 목표값 편집. */
    data class EditParam(
        val key: String,
        val value: String,
    ) : CreateChallengeIntent

    /** 인증 방식 선택. */
    data class SetVerificationType(
        val type: VerificationType,
    ) : CreateChallengeIntent

    /** 유일하게 선택 가능한 패널티. */
    data class SetWatcherPenalty(
        val enabled: Boolean,
    ) : CreateChallengeIntent

    data object VerificationPermissionsReturned : CreateChallengeIntent

    /** 텍스트 입력에서 포커스가 빠졌다. */
    data class ConfirmTextEdit(
        val field: TextEditField,
    ) : CreateChallengeIntent

    /** 이대로 만들기. */
    data object Create : CreateChallengeIntent

    /** 자동 인증 설정 시트에서 수집·이용에 동의하고 권한 요청을 이어간다. */
    data object ConfirmVerificationAccess : CreateChallengeIntent

    data object DismissVerificationAccess : CreateChallengeIntent
}
