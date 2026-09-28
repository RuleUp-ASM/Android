package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.AlreadyConsentedException
import com.ruleup.challenge.domain.entity.CannotWatchSelfException
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.InvitationExpiredException
import com.ruleup.challenge.domain.entity.InvitationInvalidException
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherBlockedException
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherInvitation
import com.ruleup.challenge.domain.entity.WatcherInviteCard
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.WatcherType
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 감시자: 초대 생성 (POST /challenges/{id}/watchers/invitations)
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

// 감시자: 목록 조회 (GET /challenges/{id}/watchers)
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
    // REVOKED +30일
    @SerialName("reinviteAvailableAt")
    val reinviteAvailableAt: String? = null,
)

internal fun WatcherResponse.toDomain(): Watcher =
    Watcher(
        // 미수락 초대는 관계가 아직 없어 id 가 비어 온다.
        watcherId = watcherId,
        type = WatcherType.fromValue(type) ?: WatcherType.USER,
        channel = WatcherChannel.fromValue(channel),
        status = WatcherStatus.fromValue(status) ?: WatcherStatus.INVITED,
        displayName = displayName,
        contactMasked = contactMasked,
        expiresAt = expiresAt,
        reinviteAvailableAt = reinviteAvailableAt,
    )

@Serializable
data class WatcherSlotsResponse(
    @SerialName("used")
    val used: Int? = null,
    // 무료 3, 구독이면 null(무제한)
    @SerialName("freeLimit")
    val freeLimit: Int? = null,
    @SerialName("subscribed")
    val subscribed: Boolean? = null,
)

/** 감시자 목록 응답. */
@Serializable
data class WatchersResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("slots")
    val slots: WatcherSlotsResponse? = null,
    // 구 계약의 평평한 한도.
    @SerialName("limit")
    val limit: Int? = null,
    @SerialName("watchers")
    val watchers: List<WatcherResponse>? = null,
    @SerialName("items")
    val items: List<WatcherResponse>? = null,
)

internal fun WatchersResponse.toDomain(): ChallengeWatchers =
    ChallengeWatchers(
        // 구독 중이면 한도가 없다
        limit = if (slots?.subscribed == true) null else slots?.freeLimit ?: limit,
        // 한 행이 망가져도 나머지는 세운다
        watchers = (watchers ?: items).orEmpty().mapNotNull { runCatching { it.toDomain() }.getOrNull() },
    )

// 초대 링크 진입(GET /watchers/invitations/{token})과 수락은 웹 동의 페이지가 담당한다

// 내가 감시자로 등록된 관계 (GET /users/me/watching — 조회 전용)
@Serializable
data class WatchingItemResponse(
    @SerialName("watcherId")
    val watcherId: String? = null,
    @SerialName("challengeTitle")
    val challengeTitle: String? = null,
    // 서버가 내리는 현행 이름.
    @SerialName("targetNickname")
    val targetNickname: String? = null,
    @SerialName("ownerNickname")
    val ownerNickname: String? = null,
    // CONSENTED / ACTIVE / REVOKED
    @SerialName("status")
    val status: String? = null,
    @SerialName("pushEnabled")
    val pushEnabled: Boolean? = null,
    @SerialName("acceptedAt")
    val acceptedAt: String? = null,
    @SerialName("consentAt")
    val consentAt: String? = null,
)

@Serializable
data class WatchingListResponse(
    @SerialName("items")
    val items: List<WatchingItemResponse>? = null,
)

internal fun WatchingListResponse.toDomain(): List<Watching> = items.orEmpty().mapNotNull { it.toDomain() }

/** 식별자가 없으면 목록 키로 쓸 값이 없어 행을 뺀다. */
internal fun WatchingItemResponse.toDomain(): Watching? {
    val id = watcherId ?: return null
    return Watching(
        watcherId = id,
        challengeTitle = challengeTitle.orEmpty(),
        ownerNickname = (targetNickname ?: ownerNickname).orEmpty(),
        status = WatcherStatus.fromValue(status),
        // 모르면 켜져 있다고 본다
        pushEnabled = pushEnabled ?: true,
        consentAt = acceptedAt ?: consentAt,
    )
}

// 초대 수락 (POST /watchers/invitations/{token}/accept)
@Serializable
data class WatcherAcceptResponse(
    @SerialName("watcherId")
    val watcherId: String? = null,
    // CONSENTED(시작 전) / ACTIVE(진행 중)
    @SerialName("status")
    val status: String? = null,
    // 인앱 수락이므로 항상 IN_APP
    @SerialName("channel")
    val channel: String? = null,
)

internal fun WatcherAcceptResponse.toDomain(): WatcherAcceptance =
    WatcherAcceptance(
        watcherId = watcherId.requireField("watcherId"),
        status = WatcherStatus.fromValue(status),
        channel = WatcherChannel.fromValue(channel),
    )

/** 수락 실패를 화면이 분기할 수 있는 타입으로 옮긴다. */
internal fun ApiException.toAcceptFailure(): Throwable =
    when (code) {
        "INVITATION_EXPIRED" -> InvitationExpiredException()
        "ALREADY_CONSENTED", "ALREADY_WATCHER" -> AlreadyConsentedException()
        "CANNOT_WATCH_SELF" -> CannotWatchSelfException()
        "WATCHER_BLOCKED" -> WatcherBlockedException()
        "INVITATION_INVALID", "INVITATION_NOT_FOUND" -> InvitationInvalidException()
        else -> this
    }
