package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier

/** 참여 형태 (명세 `mode`). 구 `participationType` 을 대체한다. */
enum class ChallengeMode(
    val value: String,
) {
    SOLO("SOLO"),
    GROUP("GROUP"),
    ;

    companion object {
        fun fromValue(value: String?): ChallengeMode? = entries.find { it.value == value }
    }
}

/** 공개 범위 (명세 `visibility`). 그룹 전용 — 솔로는 null 이다. PRIVATE 은 초대 링크로만 입장한다. */
enum class ChallengeVisibility(
    val value: String,
) {
    PUBLIC("PUBLIC"),
    PRIVATE("PRIVATE"),
    ;

    companion object {
        fun fromValue(value: String?): ChallengeVisibility? = entries.find { it.value == value }
    }
}

/** 챌린지 생애주기 (명세 `status`). 기간 만료 후 자동 삭제는 배치가 처리하므로 클라는 404 로만 인지한다. */
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

/**
 * 항목별 모더레이션 상태 (명세 `moderation.{title,description,image}`).
 *
 * **심사 중에도 모집·입장·인증에 제한이 없다** — 구 명세의 `CHALLENGE_UNDER_REVIEW` 모집 차단은 폐기됐다.
 * 상태는 방장 본인 화면의 뱃지 표시에만 쓴다.
 */
enum class ModerationState(
    val value: String,
) {
    // 심사 대상 아님 — AI·템플릿 생성본을 고치지 않고 그대로 썼다
    EXEMPT("EXEMPT"),
    APPROVED("APPROVED"),
    IN_REVIEW("IN_REVIEW"),
    REJECTED("REJECTED"),

    // 이미지 미등록
    NONE("NONE"),
    ;

    /** 방장 본인 화면에 "심사중" 뱃지를 붙일지. */
    val showsBadge: Boolean
        get() = this == IN_REVIEW || this == REJECTED

    companion object {
        fun fromValue(value: String?): ModerationState = entries.find { it.value == value } ?: NONE
    }
}

/** 제목·설명·이미지의 심사 상태 묶음 (명세 `moderation`). 방장 본인 조회에서만 내려온다. */
data class ChallengeModeration(
    val title: ModerationState,
    val description: ModerationState,
    val image: ModerationState,
)

/** 챌린지 기간 (명세 `period`). 사이클은 1주 고정이라 주기 필드는 계약에 없다. */
data class ChallengePeriod(
    // ISO date
    val start: String,
    // ISO date
    val end: String,
    // 공개 상세에서만 동반. 목록은 dday 를 따로 준다.
    val remainingDays: Int? = null,
)

/**
 * 패널티 설정 (명세 `penalties`).
 *
 * [score]·[groupShare] 는 **서버가 강제**한다 — score 는 AUTO 방이면 on, groupShare 는 GROUP 이면 on 이며
 * 클라가 무엇을 보내든 무시된다. UI 에는 셋 다 노출하되 이 둘은 잠근 채 보여준다(인지 목적).
 * 사용자가 고를 수 있는 건 [watcher] 하나뿐이다.
 */
data class ChallengePenalties(
    val score: Boolean,
    val groupShare: Boolean,
    val watcher: Boolean,
)

/**
 * 생성 초안 (명세 `draft`). `POST /challenges/draft` · `by-template` · `clone` 이 **같은 스키마**로 준다 —
 * 확인 화면과 폼 채움 로직을 그대로 재사용한다.
 *
 * 이 값은 **기본값일 뿐**이다. 사용자가 확인 화면에서 고친 뒤 생성 요청에 담은 값이 최종값이다.
 */
data class ChallengeDraft(
    val title: String,
    val description: String,
    val category: Category?,
    val mode: ChallengeMode,
    // 그룹만 — 솔로는 null
    val visibility: ChallengeVisibility?,
    // 솔로만 — 그룹은 null
    val rankingVisible: Boolean?,
    val capacity: Int,
    // 기본·상한 모두 생성자 표시 티어
    val minTier: Tier?,
    val period: ChallengePeriod,
    val params: List<ParamSpec>,
    val verification: VerificationConfig,
    val penalties: ChallengePenalties,
)

/**
 * 초안 생성 결과 (명세 `POST /challenges/draft` 200).
 *
 * [Fallback] 은 **에러가 아니라 정상 분기**다 — HTTP 200 으로 내려오며, 화면은 입력을 지우지 않고
 * 재입력 안내 배너를 띄운다. 에러 색을 쓰면 실패로 인지돼 이탈로 이어지므로 쓰지 않는다.
 */
sealed interface DraftResult {
    data class Ok(
        // 서버 발급 초안 ID — 24시간 유효. 생성 요청에 그대로 전달한다.
        val draftId: String,
        val draft: ChallengeDraft,
        // clone 경로에서만 채워진다. 출처 노출 여부는 정책 미확정이라 표시하지 않는다.
        val sourceChallengeId: String? = null,
    ) : DraftResult

    data class Fallback(
        // 서버가 준 안내 문구. 사용자를 탓하지 않고 다음 행동을 알려주는 문장이다.
        val message: String,
    ) : DraftResult
}

/** 생성 화면 추천 칩 (명세 `GET /challenges/recommendations` items[]). 서버가 **항상 3개**를 보장한다. */
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

/**
 * 챌린지 생성 요청 (명세 `POST /challenges` request).
 *
 * **수정 여부는 보내지 않는다** — 서버가 [draftId] 로 보관 중인 원본 초안과 요청값을 대조해 심사 대상을
 * 판정한다. 클라 자가 신고(`titleEdited`)는 변조 클라이언트가 심사를 우회하던 경로라 폐기됐다.
 * AI 임시 제목·복제 출처도 서버가 draft 행에서 가져오므로 클라가 지정할 수 없다.
 */
data class CreateChallengeCommand(
    val draftId: String,
    val title: String,
    val description: String,
    // 확인 화면에서도 수정 불가 · 생성 후에도 불변
    val category: Category,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    // 1~10,000 · 그룹 전용
    val capacity: Int?,
    // ≤ 생성자 표시 티어
    val minTier: Tier?,
    val period: ChallengePeriod,
    val params: List<ParamEntry>,
    val verification: VerificationConfig,
    // 선택 가능한 유일한 패널티
    val watcherPenalty: Boolean,
    // 챌린지 이미지 업로드 API 가 발급한 URL 만 허용. null 이면 기본 이미지(심사 없음).
    val imageUrl: String?,
)

/**
 * 생성 결과 (명세 `POST /challenges` 201).
 *
 * [personalSetupRequired] 가 true 면 개인 인증 설정(앵커·대상 앱) 화면으로 보낸다 — 가입 응답과 같은 계약이다.
 */
data class CreatedChallenge(
    val challengeId: String,
    val status: ChallengeStatus,
    val moderation: ChallengeModeration,
    // 생성 직후 클라가 요청할 OS 권한은 이 안의 requiredPermissions 가 유일한 출처다.
    val verification: VerificationConfig,
    val personalSetupRequired: Boolean,
    val createdAt: String,
)

/** 챌린지 삭제 결과. 진행 중 + 본인 success 이력이 있으면 탈퇴 패널티가 트리거된다. */
data class DeleteResult(
    val penaltyApplied: Boolean,
)

/**
 * 방장 전용 설정 스냅샷 (명세 `GET /challenges/{id}/settings`).
 *
 * [editableFields] 는 **서버가 잠금 규칙으로 계산한 결과**다 — 클라이언트 판단을 최종 권위로 보지 않고
 * 이 목록 기준으로 폼을 잠근다. [version] 은 PATCH 에 그대로 되돌려 보내 충돌을 감지한다(가입·탈퇴·강퇴로
 * 참여 인원이 바뀌어도 증가하므로, 그 사이 수정 가능 범위가 바뀐 것을 잡아낸다).
 */
data class ChallengeSettings(
    val config: ChallengeConfig,
    val editableFields: Set<ChallengeField>,
    val version: Int,
    val moderation: ChallengeModeration,
)

/** 수정 폼이 다루는 필드 식별자. 서버가 모르는 값을 보내면 조용히 버린다. */
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
    PARAMS("params"),
    VERIFICATION("verification"),
    PENALTIES("penalties"),
    ;

    companion object {
        fun fromValue(value: String?): ChallengeField? = entries.find { it.value == value }
    }
}

/** 방장 화면이 보는 현재 설정값 (명세 `settings.config`). 제목·설명·이미지는 심사 대체 없이 입력 원본이다. */
data class ChallengeConfig(
    val title: String,
    val description: String,
    val imageUrl: String?,
    val category: Category?,
    val mode: ChallengeMode,
    val visibility: ChallengeVisibility?,
    val rankingVisible: Boolean?,
    val capacity: Int,
    val minTier: Tier?,
    val period: ChallengePeriod,
    val params: List<ParamSpec>,
    val verification: VerificationConfig,
    val penalties: ChallengePenalties,
)

/**
 * 챌린지 수정 입력 (명세 `PATCH /challenges/{id}` request).
 *
 * **부분 수정** — null 인 필드는 "미변경"이며 전송하지 않는다. 예외적으로 [removeImage] 가 true 면
 * `imageUrl: null` 을 명시 전송해 "기본 이미지로 되돌리기"를 뜻한다. 그 외 필드에 null 을 보내면
 * 서버가 400 `INVALID_FIELD_VALUE` 로 막는다 — "값 삭제"와 "미변경"의 모호함을 없애기 위해서다.
 *
 * [version] 은 필수다(낙관적 잠금).
 */
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
    val minTier: Tier? = null,
    val period: ChallengePeriod? = null,
    val params: List<ParamEntry>? = null,
    val verification: VerificationConfig? = null,
    val watcherPenalty: Boolean? = null,
)

/** 수정 결과 (명세 `PATCH` 200). [moderation] 은 이번 수정으로 심사가 발생한 항목만 채워진다. */
data class ChallengeUpdateResult(
    val challengeId: String,
    val moderation: ChallengeModeration?,
    val updatedFields: Set<ChallengeField>,
)

/**
 * 수정 가능 범위 밖 필드를 보냈다 (명세 409 `CHALLENGE_NOT_EDITABLE`).
 * 서버가 현재 [editableFields] 를 함께 주므로 화면은 그 기준으로 폼을 다시 그린다.
 */
class ChallengeNotEditableException(
    val editableFields: Set<ChallengeField> = emptySet(),
) : Exception("지금은 수정할 수 없는 항목이 포함되어 있습니다.")

/**
 * 설정 버전 충돌 (명세 409 `VERSION_CONFLICT`). 다른 수정이나 가입·탈퇴로 잠금 범위가 바뀌었다.
 * 화면은 settings 를 재조회해 다시 그린 뒤 재시도한다.
 */
class ChallengeVersionConflictException : Exception("설정이 변경되었습니다. 다시 불러온 뒤 시도해 주세요.")

/**
 * 반복 거부로 수정이 잠겼다 (명세 429 `MODERATION_LOCKED` — 1시간 내 3회 거부 → 1시간 잠금).
 * 화면은 [retryAfterSeconds] 로 해제 시각을 명시한다.
 */
class ModerationLockedException(
    val retryAfterSeconds: Int? = null,
) : Exception("심사 거부가 반복돼 잠시 수정할 수 없습니다.")
