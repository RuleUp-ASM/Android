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
    // ---- 입력 화면 ----

    /** 화면 진입 — 추천 루틴 3개를 불러온다. */
    data object Load : CreateChallengeIntent

    data class SetRoutineDescription(
        val description: String,
    ) : CreateChallengeIntent

    /** 경로 B: 설명으로 초안 생성(LLM). 폴백이면 이 화면에 머문다. */
    data object SubmitDescription : CreateChallengeIntent

    /** 초안 생성 취소(뒤로가기). 화면을 잠그되 빠져나갈 길은 남긴다. */
    data object CancelDrafting : CreateChallengeIntent

    /** 경로 A: 추천 칩 탭 → 템플릿 초안(LLM 미경유, 대기 없음). */
    data class SelectTemplate(
        val templateId: Long,
    ) : CreateChallengeIntent

    /** 추천 영역만 재시도. */
    data object RetryTemplates : CreateChallengeIntent

    // ---- 확인 화면 ----
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
        val capacity: Int,
    ) : CreateChallengeIntent

    data class SetMinTier(
        val tier: Tier,
    ) : CreateChallengeIntent

    data class SetPeriod(
        val start: String,
        val end: String,
    ) : CreateChallengeIntent

    /** 목표값 편집. 값은 위젯이 문자열로 만들어 올린다(kind 로 위젯을 고른다). */
    data class EditParam(
        val key: String,
        val value: String,
    ) : CreateChallengeIntent

    /** 인증 방식 선택. AUTO → MANUAL 단방향만 허용된다. */
    data class SetVerificationType(
        val type: VerificationType,
    ) : CreateChallengeIntent

    /** 유일하게 선택 가능한 패널티. */
    data class SetWatcherPenalty(
        val enabled: Boolean,
    ) : CreateChallengeIntent

    /** 화면이 OS 다이얼로그로 받은 허용 토큰을 돌려준다. */
    data class PermissionsResult(
        val granted: Set<String>,
    ) : CreateChallengeIntent

    /**
     * 텍스트 입력에서 포커스가 빠졌다. 타이핑마다 보내지 않고 **여기서 원본과 비교해 1회** 기록한다.
     * 되돌려 원문과 같아졌으면 보내지 않는다.
     */
    data class ConfirmTextEdit(
        val field: TextEditField,
    ) : CreateChallengeIntent

    /** 이대로 만들기. */
    data object Create : CreateChallengeIntent
}
