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

// 신고 접수

/** 접수 결과. */
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

// 차단 목록

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

/** id 가 비면 [requireField] 로 터뜨린다 */
internal fun BlockedUserResponse.toDomain(): BlockedUser =
    BlockedUser(
        user =
            com.ruleup.domain.entity.user
                .User(userId.requireField("blocks.users[].userId"), maskedNickname.orEmpty(), null),
        blockedAt = blockedAt,
    )

internal fun BlockedChallengeResponse.toDomain(): BlockedChallenge =
    BlockedChallenge(
        challengeId = challengeId.requireField("blocks.challenges[].challengeId"),
        maskedTitle = maskedTitle.orEmpty(),
        // 모르면 미참여로 본다
        participating = participating ?: false,
        blockedAt = blockedAt,
    )

// 에러 코드 → 화면 어휘

/** 서버 에러 코드를 화면이 아는 실패로 옮긴다. */
internal fun ApiException.toReportFailure(): ReportFailure =
    when (code) {
        "ALREADY_REPORTED" -> ReportFailure.ALREADY_REPORTED
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
