package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/** 챌린지. */
sealed interface Challenge {
    val title: String
    val category: Category?
    val imageUrl: String?
}

/** 생성·수정 입력의 허용 범위. */
object ChallengeLimits {
    // 주간 수행 횟수.
    const val WEEKLY_COUNT_MIN = 1
    const val WEEKLY_COUNT_MAX = 7

    // 그룹 정원 단계(5·30·100·300·무제한).
    val CREATE_CAPACITY_STEPS: List<Int?> = listOf(5, 30, 100, 300, null)

    /** [capacity] 이상인 가장 가까운 정원 단계. */
    fun createCapacityStepAtLeast(capacity: Int?): Int? =
        capacity?.let { wanted ->
            CREATE_CAPACITY_STEPS.filterNotNull().firstOrNull {
                it >=
                    wanted
            }
        }
}

/** 참여 형태. */
enum class ChallengeMode(
    val value: String,
) {
    SOLO("SOLO"),
    GROUP("GROUP"),
    ;

    /** 남과 함께하는 챌린지인가 */
    val isGroup: Boolean
        get() = this == GROUP

    companion object {
        fun fromValue(value: String?): ChallengeMode? = entries.find { it.value == value }
    }
}

/** 공개 범위. */
enum class ChallengeVisibility(
    val value: String,
) {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    ;

    /** 초대로만 들어올 수 있는가 */
    val isPrivate: Boolean
        get() = this == PRIVATE

    companion object {
        fun fromValue(value: String?): ChallengeVisibility? = entries.find { it.value == value }
    }
}

/** 챌린지 생애주기. */
enum class ChallengeStatus(
    val value: String,
) {
    UPCOMING("UPCOMING"),
    ACTIVE("ACTIVE"),
    COMPLETED("COMPLETED"),
    ;

    companion object {
        fun fromValue(value: String?): ChallengeStatus? = entries.find { it.value == value }
    }
}

/** 항목별 모더레이션 상태. */
enum class ModerationState {
    // 심사를 앱이 하지 않으므로 "심사 대상 아님"(EXEMPT)도 여기로 합쳐 받는다
    APPROVED,
    IN_REVIEW,
    REJECTED,

    // 이미지 미등록
    NONE,
}

/** 제목·설명·이미지의 심사 상태 묶음. */
data class ChallengeModeration(
    val title: ModerationState,
    val description: ModerationState,
    val image: ModerationState,
)

/** 챌린지 기간. */
data class ChallengePeriod(
    // ISO date
    val start: String,
    // ISO date
    val end: String,
    // 공개 상세에서만 동반.
    val remainingDays: Int? = null,
)

/** 패널티 설정. */
data class ChallengePenalties(
    val score: Boolean,
    val groupShare: Boolean,
    val watcher: Boolean,
)

/** 생성 초안. */
data class ChallengeDraft(
    override val title: String,
    override val category: Category?,
    override val imageUrl: String?,
    val description: String,
    val mode: ChallengeMode,
    // 그룹만
    val visibility: ChallengeVisibility?,
    // 솔로만
    val rankingVisible: Boolean?,
    // null 이면 무제한
    val capacity: Int?,
    // 기본·상한 모두 생성자 표시 티어
    val minTier: Tier?,
    val period: ChallengePeriod,
    /** 주간 수행 횟수 1~7. */
    val weeklyCount: Int,
    val params: List<ParamSpec>,
    val verification: VerificationConfig,
    val penalties: ChallengePenalties,
) : Challenge

/** 초안 생성 결과. */
sealed interface DraftResult {
    data class Ok(
        // 서버 발급 초안 ID
        val draftId: String,
        val draft: ChallengeDraft,
        // clone 경로에서만 채워진다.
        val sourceChallengeId: String? = null,
    ) : DraftResult

    data class Fallback(
        // 서버가 준 안내 문구.
        val message: String,
    ) : DraftResult
}

/** 생성 화면 추천 칩. */
data class RoutineTemplate(
    val templateId: Long,
    val title: String,
    val description: String?,
    val category: Category?,
    // 루틴 테이블엔 자동 인증 가능 루틴만 들어가므로 사실상 AUTO 고정
    val verificationType: VerificationType,
    // 추천 사유 표시 문구(예: "20대 인기 루틴")
    val reason: String,
)

/** 챌린지 생성 요청. */
data class CreateChallengeCommand(
    val draftId: String,
    val title: String,
    val description: String,
    // 확인 화면에서도 수정 불가 · 생성 후에도 불변
    val category: Category,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    // 그룹 전용.
    val capacity: Int?,
    // ≤ 생성자 표시 티어
    val minTier: Tier?,
    val period: ChallengePeriod,
    // 주간 수행 횟수.
    val weeklyCount: Int,
    val params: List<ParamEntry>,
    val verification: VerificationConfig,
    // 선택 가능한 유일한 패널티
    val watcherPenalty: Boolean,
    // 챌린지 이미지 업로드 API 가 발급한 URL 만 허용.
    val imageUrl: String?,
) {
    /** 범위와 [mode] 별 필드 조합을 타입이 보장한다. */
    init {
        require(weeklyCount in ChallengeLimits.WEEKLY_COUNT_MIN..ChallengeLimits.WEEKLY_COUNT_MAX) {
            "주간 횟수가 범위를 벗어났습니다: $weeklyCount"
        }
        if (mode.isGroup) {
            require(visibility != null) { "그룹 챌린지는 공개 범위가 필요합니다." }
            require(rankingVisible == null) { "랭킹 공개 여부는 솔로 전용입니다." }
            require(capacity in ChallengeLimits.CREATE_CAPACITY_STEPS) {
                "정원이 범위를 벗어났습니다: $capacity"
            }
        } else {
            require(visibility == null) { "공개 범위는 그룹 전용입니다." }
            require(rankingVisible != null) { "솔로 챌린지는 랭킹 공개 여부가 필요합니다." }
            require(capacity == null) { "정원은 그룹 전용입니다." }
        }
    }
}

/** 생성 결과. */
data class CreatedChallenge(
    val challengeId: String,
    val status: ChallengeStatus,
    val moderation: ChallengeModeration,
    // 생성 직후 클라가 요청할 OS 권한은 이 안의 requiredPermissions 가 유일한 출처다.
    val verification: VerificationConfig,
    val personalSetupRequired: Boolean,
    val createdAt: String,
)

/** 방장 전용 설정 스냅샷. */
data class ChallengeSettings(
    val config: ChallengeConfig,
    val editableFields: Set<ChallengeField>,
    val version: Int,
    val moderation: ChallengeModeration,
)

/** 수정 폼이 다루는 필드 식별자. */
enum class ChallengeField(
    val value: String,
) {
    TITLE("title"),
    DESCRIPTION("description"),
    IMAGE_URL("imageUrl"),
    MODE("mode"),
    VISIBILITY("visibility"),
    RANKING_VISIBLE("rankingVisible"),
    CAPACITY("capacity"),
    MIN_TIER("minTier"),
    PERIOD("period"),
    WEEKLY_COUNT("weeklyCount"),
    PARAMS("params"),
    VERIFICATION("verification"),
    PENALTIES("penalties"),
    ;

    companion object {
        /** 서버는 하위 경로까지 찍어 보낸다 */
        fun fromValue(value: String?): ChallengeField? {
            val head = value?.substringBefore('.')
            return entries.find { it.value == head }
        }
    }
}

/** 방장 화면이 보는 현재 설정값. */
data class ChallengeConfig(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val category: Category?,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    // null 이면 무제한
    val capacity: Int?,
    val minTier: Tier?,
    val period: ChallengePeriod,
    // 주간 수행 횟수 1~7
    val weeklyCount: Int,
    val params: List<ParamSpec>,
    val verification: VerificationConfig,
    val penalties: ChallengePenalties,
)

/** 챌린지 수정 입력. */
data class ChallengeUpdate(
    val version: Int,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val removeImage: Boolean = false,
    val mode: ChallengeMode? = null,
    val visibility: ChallengeVisibility? = null,
    val rankingVisible: Boolean? = null,
    val capacity: Int? = null,
    // true 면 `capacity: null` 을 명시 전송해 정원을 무제한으로 바꾼다
    val unlimitedCapacity: Boolean = false,
    val minTier: Tier? = null,
    val period: ChallengePeriod? = null,
    val weeklyCount: Int? = null,
    val params: List<ParamEntry>? = null,
    val verification: VerificationConfig? = null,
    val watcherPenalty: Boolean? = null,
)

/** 수정 결과. */
data class ChallengeUpdateResult(
    val challengeId: String,
    val moderation: ChallengeModeration?,
    val updatedFields: Set<ChallengeField>,
)

/** 수정 가능 범위 밖 필드를 보냈다. */
class ChallengeNotEditableException(
    val editableFields: Set<ChallengeField> = emptySet(),
) : Exception("지금은 수정할 수 없는 항목이 포함되어 있습니다.")

/** 주간 횟수가 1~7 범위를 벗어났다. */
class InvalidWeeklyCountException : Exception("주간 횟수는 1~7회 사이여야 합니다.")

/** 설정 버전 충돌. */
class ChallengeVersionConflictException : Exception("설정이 변경되었습니다. 다시 불러온 뒤 시도해 주세요.")

/** 반복 거부로 수정이 잠겼다. */
class ModerationLockedException(
    val retryAfterSeconds: Int? = null,
) : Exception("심사 거부가 반복돼 잠시 수정할 수 없습니다.")
