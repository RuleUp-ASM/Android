package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInvitationInfo
import com.ruleup.challenge.domain.entity.WatcherInvitationState
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

// ---------- 감시자: 초대 링크 진입 (GET /watchers/invitations/{token}) ----------
@Serializable
data class WatcherInvitationInfoResponse(
    @SerialName("invitationId")
    val invitationId: String? = null,
    // INVITED / CONSENTED / EXPIRED / REVOKED
    @SerialName("status")
    val status: String? = null,
    @SerialName("challengeTitle")
    val challengeTitle: String? = null,
    @SerialName("inviterNickname")
    val inviterNickname: String? = null,
    // 현재 로그인 사용자가 룰업 유저인지(웹 분기용 — 앱에서는 항상 로그인 유저)
    @SerialName("viewerIsUser")
    val viewerIsUser: Boolean? = null,
    // 생성자–감시자 30일 재초대 차단
    @SerialName("blocked")
    val blocked: Boolean? = null,
    @SerialName("blockedUntil")
    val blockedUntil: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

internal fun WatcherInvitationInfoResponse.toDomain(): WatcherInvitationInfo =
    WatcherInvitationInfo(
        challengeTitle = challengeTitle.requireField("challengeTitle"),
        inviterNickname = inviterNickname.requireField("inviterNickname"),
        // status + blocked 를 화면 분기용 단일 상태로 환원한다(차단은 200 정상 응답으로 온다).
        state =
            when {
                blocked == true -> WatcherInvitationState.BLOCKED
                status == WatcherStatus.CONSENTED.value -> WatcherInvitationState.ALREADY_ACCEPTED
                status == WatcherStatus.EXPIRED.value -> WatcherInvitationState.EXPIRED
                status == WatcherStatus.REVOKED.value -> WatcherInvitationState.REVOKED
                else -> WatcherInvitationState.PENDING
            },
        blockedUntil = blockedUntil,
    )
