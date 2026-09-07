package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeInvitation
import com.ruleup.challenge.domain.entity.ChallengeInvitationPreview
import com.ruleup.challenge.domain.entity.InvitedChallenge
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 멤버 초대 링크 발급 (POST /challenges/{id}/invitations) ----------
@Serializable
data class ChallengeInvitationResponse(
    @SerialName("invitationId")
    val invitationId: String? = null,
    @SerialName("token")
    val token: String? = null,
    // 앱링크 도메인 기반. 클라가 조립하지 않고 이 값을 그대로 공유한다
    @SerialName("inviteUrl")
    val inviteUrl: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

internal fun ChallengeInvitationResponse.toDomain(): ChallengeInvitation =
    ChallengeInvitation(
        invitationId = invitationId.requireField("invitationId"),
        token = token.requireField("token"),
        // 링크가 없으면 공유할 것이 없다 — 빈 문자열로 흘리면 빈 카톡이 나간다.
        inviteUrl = inviteUrl.requireField("inviteUrl"),
        expiresAt = expiresAt,
    )

// ---------- 초대 링크 미리보기 (GET /challenges/invitations/{token}) ----------
@Serializable
data class InvitedChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
)

@Serializable
data class ChallengeInvitationPreviewResponse(
    @SerialName("invitationId")
    val invitationId: String? = null,
    @SerialName("challenge")
    val challenge: InvitedChallengeResponse? = null,
    @SerialName("inviterNickname")
    val inviterNickname: String? = null,
    @SerialName("joinable")
    val joinable: Boolean? = null,
    @SerialName("blockReason")
    val blockReason: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

internal fun ChallengeInvitationPreviewResponse.toDomain(): ChallengeInvitationPreview =
    ChallengeInvitationPreview(
        invitationId = invitationId.orEmpty(),
        challenge = challenge.requireField("challenge").toDomain(),
        inviterNickname = inviterNickname,
        // 모르면 막힌 것으로 본다 — 못 들어갈 방을 열려 있는 것처럼 보여 주면 눌렀다가 에러를 본다.
        joinable = joinable ?: false,
        blockReason = JoinBlockReason.fromValue(blockReason),
        expiresAt = expiresAt,
    )

internal fun InvitedChallengeResponse.toDomain(): InvitedChallenge =
    InvitedChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        imageUrl = imageUrl,
        category = Category.fromValue(category.orEmpty()),
        participantCount = participantCount ?: 0,
        capacity = capacity ?: 0,
        minTier = minTier?.let(Tier::fromValue),
        startDate = startDate,
        endDate = endDate,
    )
