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
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.mvi.UiState

/**
 * 챌린지 생성 플로우 상태. 입력 화면과 확인 화면이 같은 인스턴스를 공유한다.
 *
 * 초안([original])은 **불변으로 보관**하고 편집본을 따로 들고 있는다 — 어떤 항목이 원본과 달라졌는지
 * 뱃지로 보여주기 위해서다. 다만 **심사 대상 판정은 서버가 `draftId` 원본 대조로** 하므로 이 값은
 * 화면 표시·로깅에만 쓰고 생성 요청에는 싣지 않는다.
 */
data class CreateChallengeState(
    // ---- 입력 화면 ----
    val routineDescription: String,
    val isDrafting: Boolean,
    // 폴백 안내. 에러가 아니므로 에러 색을 쓰지 않는다 — 실패로 인지되면 이탈로 이어진다.
    val fallbackMessage: String?,
    // 429 카운트다운. null 이 아니면 "다음" 버튼을 잠근다.
    val retryAfterSeconds: Int?,
    val templates: List<RoutineTemplate>,
    val isLoadingTemplates: Boolean,
    // 추천 영역만 재시도 — 설명 입력 경로는 계속 쓸 수 있어야 한다.
    val templatesFailed: Boolean,
    // ---- 확인 화면 ----
    val draftId: String?,
    val original: ChallengeDraft?,
    val title: String,
    val description: String,
    // 확인 화면부터 수정 불가 · 생성 후에도 불변
    val category: Category?,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    val capacity: Int,
    val minTier: Tier?,
    // minTier 슬라이더 상한 = 생성자 표시 티어(초안이 준 기본값). 진입 시점 값으로 고정한다.
    val ownerTierCap: Tier?,
    val period: ChallengePeriod,
    // 주간 수행 횟수 1~7. 요일이 아니라 "그 주에 몇 번" 이다(구 repeatDays 대체).
    val weeklyCount: Int,
    val params: List<ParamSpec>,
    val verification: VerificationConfig?,
    val penalties: ChallengePenalties,
    val coverImageUri: String?,
    // 확인 화면 진입 시 1회 생성해 재시도까지 재사용한다.
    val idempotencyKey: String?,
    val grantedPermissions: Set<String>,
    val permissionRequested: Boolean,
    val isCreating: Boolean,
    // 생성은 끝났고 권한 요청 응답만 기다리는 상태. 권한 결과를 받으면 홈으로 나간다.
    val createdChallengeId: String?,
) : UiState {
    /** 초안이 도착해 확인 화면을 그릴 수 있는 상태인지. */
    val hasDraft: Boolean
        get() = draftId != null

    /** 제목을 사용자가 고쳤는지 — AI 생성 뱃지 노출용. 되돌리면 다시 false 가 된다. */
    val titleEdited: Boolean
        get() = original != null && original.title != title

    /** 설명을 사용자가 고쳤는지. */
    val descriptionEdited: Boolean
        get() = original != null && original.description != description

    val isGroup: Boolean
        get() = mode.isGroup

    /**
     * 자동 인증을 고를 수 있는지. 기준은 **초안**이라 수동으로 바꿨어도 다시 켤 수 있다 —
     * 단방향 잠금은 생성 이후 수정 화면의 규칙이다.
     */
    val canUseAuto: Boolean
        get() = original?.verification?.type?.isAuto == true

    /** 지금 자동 인증이 선택돼 있는지. */
    val isAuto: Boolean
        get() = verification?.type?.isAuto == true

    /** 설명 입력으로 초안을 만들 수 있는 상태인지. */
    val canSubmitDescription: Boolean
        get() = routineDescription.trim().isNotEmpty() && !isDrafting && retryAfterSeconds == null

    companion object {
        // 명세가 제목 길이를 정하지 않았다. 무한 입력을 막는 클라이언트 가드일 뿐이라 서버 검증과 무관하다.
        const val TITLE_MAX = 30

        // 명세: 루틴 설명 1~200자. 서버가 같은 범위를 재검증한다.

        // 범위는 도메인이 정한다([ChallengeLimits]). 여기 남는 건 화면 기본값뿐이다.
        const val CAPACITY_DEFAULT = 50

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
                grantedPermissions = emptySet(),
                permissionRequested = false,
                isCreating = false,
                createdChallengeId = null,
            )
    }
}
