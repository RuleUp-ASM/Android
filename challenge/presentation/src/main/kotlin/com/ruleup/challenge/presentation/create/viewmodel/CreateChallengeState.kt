package com.ruleup.challenge.presentation.create.viewmodel

import com.ruleup.challenge.domain.entity.ChallengeDraft
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePenalties
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.isInRange
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.mvi.UiState
import com.ruleup.verification.domain.entity.VerificationAccess

/** 챌린지 생성 플로우 상태. */
data class CreateChallengeState(
    // 입력 화면
    val routineDescription: String,
    val isDrafting: Boolean,
    // 폴백 안내.
    val fallbackMessage: String?,
    // 429 카운트다운.
    val retryAfterSeconds: Int?,
    val templates: List<RoutineTemplate>,
    val isLoadingTemplates: Boolean,
    // 추천 영역만 재시도
    val templatesFailed: Boolean,
    // 「다른 추천 보기」로 지금까지 본 추천 — 다음 요청에서 뺀다
    val seenTemplateIds: Set<Long> = emptySet(),
    // 확인 화면
    val draftId: String?,
    val original: ChallengeDraft?,
    val title: String,
    val description: String,
    // 확인 화면부터 수정 불가 · 생성 후에도 불변
    val category: Category?,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    // 그룹 정원.
    val capacity: Int?,
    val minTier: Tier?,
    // minTier 슬라이더 상한 = 생성자 표시 티어(초안이 준 기본값).
    val ownerTierCap: Tier?,
    val period: ChallengePeriod,
    // 주간 수행 횟수 1~7.
    val weeklyCount: Int,
    val params: List<ParamSpec>,
    val verification: VerificationConfig?,
    val penalties: ChallengePenalties,
    val coverImageUri: String?,
    // 확인 화면 진입 시 1회 생성해 재시도까지 재사용한다.
    val idempotencyKey: String?,
    val isCreating: Boolean,
    // 생성은 끝났고 권한 요청 응답만 기다리는 상태.
    val createdChallengeId: String?,
    // 생성에 필요한 동의와 권한.
    val pendingAccess: VerificationAccess? = null,
    val isAccessSubmitting: Boolean = false,
) : UiState {
    /** 초안이 도착해 확인 화면을 그릴 수 있는 상태인지. */
    val hasDraft: Boolean
        get() = draftId != null

    /** 목표값이 전부 서버가 준 허용 범위 안인지. */
    val paramsInRange: Boolean
        get() = params.all { it.isInRange }

    /** 제목을 사용자가 고쳤는지 */
    val titleEdited: Boolean
        get() = original != null && original.title != title

    /** 설명을 사용자가 고쳤는지. */
    val descriptionEdited: Boolean
        get() = original != null && original.description != description

    val isGroup: Boolean
        get() = mode.isGroup

    /** 자동 인증을 고를 수 있는지. */
    val canUseAuto: Boolean
        get() =
            original

                ?.verification
                ?.type
                ?.isAuto == true

    /** 지금 자동 인증이 선택돼 있는지. */
    val isAuto: Boolean
        get() = verification?.type?.isAuto == true

    /** 설명 입력으로 초안을 만들 수 있는 상태인지. */
    val canSubmitDescription: Boolean
        get() = routineDescription.trim().isNotEmpty() && !isDrafting && retryAfterSeconds == null

    companion object {
        // 명세가 제목 길이를 정하지 않았다.
        const val TITLE_MAX = 30

        // 명세: 루틴 설명 1~200자.

        // 범위는 도메인이 정한다([ChallengeLimits]).
        const val CAPACITY_DEFAULT = 30

        val initial =
            CreateChallengeState(
                routineDescription = "",
                isDrafting = false,
                fallbackMessage = null,
                retryAfterSeconds = null,
                templates = emptyList(),
                isLoadingTemplates = false,
                templatesFailed = false,
                draftId = null,
                original = null,
                title = "",
                description = "",
                category = null,
                mode = ChallengeMode.SOLO,
                visibility = null,
                rankingVisible = true,
                capacity = CAPACITY_DEFAULT,
                minTier = null,
                ownerTierCap = null,
                period = ChallengePeriod(start = "", end = ""),
                // 기본 7회 = 매일
                weeklyCount = ChallengeLimits.WEEKLY_COUNT_MAX,
                params = emptyList(),
                verification = null,
                penalties = ChallengePenalties(score = false, groupShare = false, watcher = false),
                coverImageUri = null,
                idempotencyKey = null,
                isCreating = false,
                createdChallengeId = null,
            )
    }
}
