package com.ruleup.challenge.domain.entity

/** 공지 본문 상한 (명세 8.1). 서버 검증과 동일 값 — 작성/수정 화면의 입력 제한에 사용한다. */
object NoticePolicy {
    const val TITLE_MAX_LENGTH = 100
    const val CONTENT_MAX_LENGTH = 2000
}

/**
 * 공지 목록 항목 (명세: GET notices — 고정 우선 → 최신순, 서버 고정 최근 10건).
 * 방 홈의 고정 공지 배너([ChallengeRoom.pinnedNotice])에도 재사용한다 — 그 경우 [preview] 는 빈 문자열.
 */
data class NoticeSummary(
    val noticeId: String,
    val title: String,
    // 본문 80자 요약
    val preview: String,
    val pinned: Boolean,
    val isRead: Boolean,
    // ISO datetime
    val createdAt: String,
)

/** 공지 상세 (명세: GET notices/{id} — 서버가 조회 시점에 읽음 upsert, 별도 읽음 API 없음). */
data class NoticeDetail(
    val noticeId: String,
    val title: String,
    val content: String,
    val pinned: Boolean,
    // visibleNicknameTo 적용된 작성자 닉네임
    val authorNickname: String,
    val authorProfileImageUrl: String?,
    val createdAt: String,
    // 수정된 적 없으면 null
    val updatedAt: String?,
)

/** 공지 작성 결과 (명세: POST notices — pinned=true 생성 시 기존 고정 자동 해제). */
data class NoticeCreateResult(
    val noticeId: String,
    val pinned: Boolean,
)

/** 공지 수정 결과 (명세: PUT notices/{id}). 요청 resetRead=true 면 전 멤버 읽음 초기화 + 재발송. */
data class NoticeUpdateResult(
    val noticeId: String,
    // 읽음 초기화 수행 여부 (응답 필드명 readReset — 요청 resetRead 와 다름에 주의)
    val readReset: Boolean,
)

/** 공지 고정/해제 결과 (명세: PATCH pin — 단일 pin, 새로 고정하면 기존 고정 자동 해제). */
data class NoticePinResult(
    val noticeId: String,
    val pinned: Boolean,
    // 자동 해제된 기존 고정 공지 (없으면 null) — 목록 화면의 로컬 상태 갱신용
    val unpinnedNoticeId: String?,
)
