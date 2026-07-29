package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.WatcherType
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 감시자: 초대 생성 (POST /challenges/{id}/watchers/invitations) ----------
@Serializable
data class KakaoShareCardResponse(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("buttonLabel")
    val buttonLabel: String? = null,
)

internal fun KakaoShareCardResponse.toDomainOrNull(): WatcherInviteCard? {
    val cardTitle = title ?: return null
    return WatcherInviteCard(
        title = cardTitle,
        description = description.orEmpty(),
        buttonLabel = buttonLabel ?: "수락하기",
    )
}

@Serializable
data class WatcherInvitationResponse(
    @SerialName("invitationId")
    val invitationId: String? = null,
    @SerialName("token")
    val token: String? = null,
    // 카카오톡 카드 버튼에 실을 초대 링크(웹 동의 페이지 겸용)
    @SerialName("inviteUrl")
    val inviteUrl: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
    // 카톡 공유 카드 문구(서버 제공)
    @SerialName("kakaoShare")
    val kakaoShare: KakaoShareCardResponse? = null,
)

internal fun WatcherInvitationResponse.toDomain(): WatcherInvitation =
    WatcherInvitation(
        invitationId = invitationId,
        token = token.requireField("token"),
        inviteUrl = inviteUrl.requireField("inviteUrl"),
        expiresAt = expiresAt,
        kakaoShare = kakaoShare?.toDomainOrNull(),
    )

// ---------- 감시자: 목록 조회 (GET /challenges/{id}/watchers) ----------
@Serializable
data class WatcherResponse(
    @SerialName("watcherId")
    val watcherId: String? = null,
    // USER / NON_USER
    @SerialName("type")
    val type: String? = null,
    // IN_APP / SMS
    @SerialName("channel")
    val channel: String? = null,
    @SerialName("status")
    val status: String? = null,
    // 유저면 닉네임, 비유저면 null
    @SerialName("displayName")
    val displayName: String? = null,
    // 비유저 마스킹 연락처(초대한 참여자에게 원본 미노출)
    @SerialName("contactMasked")
    val contactMasked: String? = null,
    @SerialName("invitedAt")
    val invitedAt: String? = null,
    // INVITED 일 때 토큰 만료
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

internal fun WatcherResponse.toDomain(): Watcher =
    Watcher(
        watcherId = watcherId.requireField("watcherId"),
        type = WatcherType.fromValue(type) ?: WatcherType.USER,
        channel = WatcherChannel.fromValue(channel),
        status = WatcherStatus.fromValue(status) ?: WatcherStatus.INVITED,
        displayName = displayName,
        contactMasked = contactMasked,
        expiresAt = expiresAt,
    )

@Serializable
data class WatchersResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    // 무료 3, 구독 시 null(무제한)
    @SerialName("limit")
    val limit: Int? = null,
    @SerialName("watchers")
    val watchers: List<WatcherResponse>? = null,
)

internal fun WatchersResponse.toDomain(): ChallengeWatchers =
    ChallengeWatchers(
        limit = limit,
        watchers = watchers.orEmpty().map { it.toDomain() },
    )

// 초대 링크 진입(GET /watchers/invitations/{token})과 수락은 웹 동의 페이지가 담당한다 — 앱 DTO 없음.
