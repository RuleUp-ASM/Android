package com.ruleup.report.data.dto

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.requireField
import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.BlockedChallenge
import com.ruleup.report.domain.entity.BlockedUser
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.entity.ReportResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 신고 접수 ----------

/**
 * 접수 결과(명세 POST /reports 201).
 *
 * 응답의 `blocked` 는 선언하지 않는다 — 유저·챌린지 신고 모두 항상 true 라 읽을 이유가 없고,
 * 파서가 모르는 키를 무시하도록 설정돼 있어 남겨 둘 필요도 없다.
 */
@Serializable
data class ReportCreateResponse(
    @SerialName("reportId")
    val reportId: String? = null,
    @SerialName("hiddenEffect")
    val hiddenEffect: String? = null,
)

internal fun ReportCreateResponse.toDomain(): ReportResult =
    ReportResult(
        // 접수 식별자가 없으면 무엇이 접수됐는지 말할 수 없다.
        reportId = reportId.requireField("reportId"),
        hiddenEffect = HiddenEffect.fromValue(hiddenEffect),
    )

// ---------- 차단 목록 ----------

@Serializable
data class BlockListResponse(
    @SerialName("users")
    val users: List<BlockedUserResponse>? = null,
    @SerialName("challenges")
    val challenges: List<BlockedChallengeResponse>? = null,
)

@Serializable
data class BlockedUserResponse(
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("maskedNickname")
    val maskedNickname: String? = null,
    @SerialName("blockedAt")
    val blockedAt: String? = null,
)

@Serializable
data class BlockedChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("maskedTitle")
    val maskedTitle: String? = null,
    @SerialName("participating")
    val participating: Boolean? = null,
    @SerialName("blockedAt")
    val blockedAt: String? = null,
)

internal fun BlockListResponse.toDomain(): BlockList =
    BlockList(
        users = users.orEmpty().map { it.toDomain() },
        challenges = challenges.orEmpty().map { it.toDomain() },
    )

/**
 * id 가 비면 [requireField] 로 터뜨린다 — 그 행만 조용히 빼면 사용자는 차단이 남아 있는데
 * 목록에서 사라진 상대를 영영 풀 수 없게 된다. 목록을 못 그리는 편이 눈에 띄고 고칠 수 있다.
 */
internal fun BlockedUserResponse.toDomain(): BlockedUser =
    BlockedUser(
        userId = userId.requireField("blocks.users[].userId"),
        maskedNickname = maskedNickname.orEmpty(),
        blockedAt = blockedAt,
    )

internal fun BlockedChallengeResponse.toDomain(): BlockedChallenge =
    BlockedChallenge(
        challengeId = challengeId.requireField("blocks.challenges[].challengeId"),
        maskedTitle = maskedTitle.orEmpty(),
        // 모르면 미참여로 본다 — 참여 중이라고 잘못 말하면 "방에서 나가기"를 권하게 된다.
        participating = participating ?: false,
        blockedAt = blockedAt,
    )

// ---------- 에러 코드 → 화면 어휘 ----------

/**
 * 서버 에러 코드를 화면이 아는 실패로 옮긴다. 여기 없는 코드는 [ReportFailure.UNKNOWN] 이고,
 * 화면은 일반 오류 문구를 쓴다 — 서버가 코드를 추가해도 앱이 멈추지 않는다.
 */
internal fun ApiException.toReportFailure(): ReportFailure =
    when (code) {
        "REPORT_SUSPENDED" -> ReportFailure.SUSPENDED
        "CANNOT_REPORT_SELF" -> ReportFailure.SELF_TARGET
        "INVALID_REPORT_TARGET" -> ReportFailure.INVALID_TARGET
        "INVALID_REPORT_REASON" -> ReportFailure.INVALID_REASON
        // 서버는 대상 종류별로 코드를 나누지만 화면이 할 일은 "이미 없는 대상"으로 같다.
        "USER_NOT_FOUND", "CHALLENGE_NOT_FOUND" -> ReportFailure.TARGET_NOT_FOUND
        "ACCOUNT_LOCKED" -> ReportFailure.ACCOUNT_LOCKED
        "BLOCK_ENTRY_NOT_FOUND" -> ReportFailure.BLOCK_ENTRY_NOT_FOUND
        else -> ReportFailure.UNKNOWN
    }
