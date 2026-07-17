package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.NoticeCreateResult
import com.ruleup.challenge.domain.entity.NoticeDetail
import com.ruleup.challenge.domain.entity.NoticePinResult
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.entity.NoticeUpdateResult
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 공지: 목록 조회 (GET /challenges/{id}/notices) · 방 홈 pinnedNotice ----------
@Serializable
data class NoticeSummaryResponse(
    @SerialName("noticeId")
    val noticeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    // 본문 80자 요약 — 방 홈 pinnedNotice 에는 내려오지 않는다
    @SerialName("preview")
    val preview: String? = null,
    @SerialName("pinned")
    val pinned: Boolean? = null,
    @SerialName("isRead")
    val isRead: Boolean? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

internal fun NoticeSummaryResponse.toDomain(): NoticeSummary =
    NoticeSummary(
        noticeId = noticeId.requireField("noticeId"),
        title = title.orEmpty(),
        preview = preview.orEmpty(),
        pinned = pinned ?: false,
        isRead = isRead ?: false,
        createdAt = createdAt.orEmpty(),
    )

@Serializable
data class NoticesResponse(
    @SerialName("notices")
    val notices: List<NoticeSummaryResponse>? = null,
)

// ---------- 공지: 상세 조회 + 읽음 처리 (GET /challenges/{id}/notices/{noticeId}) ----------
@Serializable
data class NoticeAuthorResponse(
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
)

@Serializable
data class NoticeDetailResponse(
    @SerialName("noticeId")
    val noticeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("content")
    val content: String? = null,
    @SerialName("pinned")
    val pinned: Boolean? = null,
    @SerialName("author")
    val author: NoticeAuthorResponse? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
    // 수정된 적 없으면 null
    @SerialName("updatedAt")
    val updatedAt: String? = null,
)

internal fun NoticeDetailResponse.toDomain(): NoticeDetail =
    NoticeDetail(
        noticeId = noticeId.requireField("noticeId"),
        title = title.orEmpty(),
        content = content.orEmpty(),
        pinned = pinned ?: false,
        authorNickname = author?.nickname.orEmpty(),
        authorProfileImageUrl = author?.profileImageUrl,
        createdAt = createdAt.orEmpty(),
        updatedAt = updatedAt,
    )

// ---------- 공지: 작성 (POST /challenges/{id}/notices) ----------
@Serializable
data class CreateNoticeRequest(
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
    @SerialName("pinned")
    val pinned: Boolean,
)

@Serializable
data class CreateNoticeResponse(
    @SerialName("noticeId")
    val noticeId: String? = null,
    @SerialName("pinned")
    val pinned: Boolean? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

internal fun CreateNoticeResponse.toDomain(): NoticeCreateResult =
    NoticeCreateResult(
        noticeId = noticeId.requireField("noticeId"),
        pinned = pinned ?: false,
    )

// ---------- 공지: 수정 (PUT /challenges/{id}/notices/{noticeId}) ----------
@Serializable
data class UpdateNoticeRequest(
    @SerialName("title")
    val title: String,
    @SerialName("content")
    val content: String,
    // true = 전 멤버 읽음 초기화 + 재발송 (규칙 변경 등)
    @SerialName("resetRead")
    val resetRead: Boolean,
)

@Serializable
data class UpdateNoticeResponse(
    @SerialName("noticeId")
    val noticeId: String? = null,
    @SerialName("updatedAt")
    val updatedAt: String? = null,
    // 요청 필드명은 resetRead, 응답은 readReset (명세 그대로)
    @SerialName("readReset")
    val readReset: Boolean? = null,
)

internal fun UpdateNoticeResponse.toDomain(): NoticeUpdateResult =
    NoticeUpdateResult(
        noticeId = noticeId.requireField("noticeId"),
        readReset = readReset ?: false,
    )

// ---------- 공지: 고정/해제 (PATCH /challenges/{id}/notices/{noticeId}/pin) ----------
@Serializable
data class PinNoticeRequest(
    @SerialName("pinned")
    val pinned: Boolean,
)

@Serializable
data class PinNoticeResponse(
    @SerialName("noticeId")
    val noticeId: String? = null,
    @SerialName("pinned")
    val pinned: Boolean? = null,
    // 단일 pin — 자동 해제된 기존 고정 공지
    @SerialName("unpinnedNoticeId")
    val unpinnedNoticeId: String? = null,
)

internal fun PinNoticeResponse.toDomain(): NoticePinResult =
    NoticePinResult(
        noticeId = noticeId.requireField("noticeId"),
        pinned = pinned ?: false,
        unpinnedNoticeId = unpinnedNoticeId,
    )
