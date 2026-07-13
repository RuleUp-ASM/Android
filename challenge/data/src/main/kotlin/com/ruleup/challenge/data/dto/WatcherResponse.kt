package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInvitationInfo
import com.ruleup.challenge.domain.entity.WatcherInvitationState
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 감시자: 초대 생성 (POST /challenges/{id}/watchers/invitations) ----------
@Serializable
data class WatcherInvitationResponse(
    @SerialName("token")
    val token: String? = null,
    // 카카오톡 카드 버튼에 실을 초대 링크(웹 동의 페이지 겸용)
    @SerialName("inviteUrl")
    val inviteUrl: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

internal fun WatcherInvitationResponse.toDomain(): WatcherInvitation =
    WatcherInvitation(
        token = token.requireField("token"),
        inviteUrl = inviteUrl.requireField("inviteUrl"),
        expiresAt = expiresAt,
    )

// ---------- 감시자: 목록 조회 (GET /challenges/{id}/watchers) ----------
@Serializable
data class WatcherResponse(
    @SerialName("watcherId")
    val watcherId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    // 비유저 감시자의 마스킹 연락처(생성자에게 원본 미노출)
    @SerialName("maskedContact")
    val maskedContact: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("channel")
    val channel: String? = null,
)

internal fun WatcherResponse.toDomain(): Watcher =
    Watcher(
        watcherId = watcherId.requireField("watcherId"),
        nickname = nickname,
        maskedContact = maskedContact,
        status = WatcherStatus.fromValue(status) ?: WatcherStatus.INVITED,
        channel = WatcherChannel.fromValue(channel),
    )

@Serializable
data class WatchersResponse(
    @SerialName("watchers")
    val watchers: List<WatcherResponse>? = null,
)

internal fun WatchersResponse.toDomain(): List<Watcher> = watchers.orEmpty().map { it.toDomain() }

// ---------- 감시자: 초대 링크 진입 (GET /watchers/invitations/{token}) ----------
@Serializable
data class WatcherInvitationInfoResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("challengeTitle")
    val challengeTitle: String? = null,
    @SerialName("ownerNickname")
    val ownerNickname: String? = null,
    // PENDING / EXPIRED / ALREADY_ACCEPTED / BLOCKED
    @SerialName("state")
    val state: String? = null,
)

internal fun WatcherInvitationInfoResponse.toDomain(): WatcherInvitationInfo =
    WatcherInvitationInfo(
        challengeId = challengeId.requireField("challengeId"),
        challengeTitle = challengeTitle.requireField("challengeTitle"),
        ownerNickname = ownerNickname.requireField("ownerNickname"),
        state = WatcherInvitationState.fromValue(state) ?: WatcherInvitationState.EXPIRED,
    )
