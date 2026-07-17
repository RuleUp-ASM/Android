package com.ruleup.profile.data.dto

import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.FriendInvitation
import com.ruleup.profile.domain.entity.FriendInvitee
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 친구 초대 정보 (GET /me/invitation) ----------
@Serializable
data class FriendInviteeResponse(
    @SerialName("nickname")
    val nickname: String? = null,
    // 현재 SIGNED_UP 단일 값
    @SerialName("status")
    val status: String? = null,
    @SerialName("occurredAt")
    val occurredAt: String? = null,
)

@Serializable
data class FriendInvitationResponse(
    @SerialName("inviteCode")
    val inviteCode: String? = null,
    @SerialName("inviteUrl")
    val inviteUrl: String? = null,
    @SerialName("rewardDescription")
    val rewardDescription: String? = null,
    @SerialName("invitees")
    val invitees: List<FriendInviteeResponse>? = null,
)

internal fun FriendInvitationResponse.toDomain(): FriendInvitation =
    FriendInvitation(
        inviteCode = inviteCode.requireField("inviteCode"),
        inviteUrl = inviteUrl.requireField("inviteUrl"),
        rewardDescription = rewardDescription.orEmpty(),
        invitees =
            invitees.orEmpty().map {
                FriendInvitee(
                    nickname = it.nickname.orEmpty(),
                    status = it.status.orEmpty(),
                    occurredAt = it.occurredAt.orEmpty(),
                )
            },
    )
