package com.ruleup.challenge.domain.entity

/** 스레드 페이징 (명세 `size` — 기본 20 · 최대 50). */
object ThreadPolicy {
    const val PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50
}

/**
 * 피드 아이템 종류 (명세 `items[].type`).
 *
 * Phase 1 에서 서버는 `VERIFY_SUCCESS`/`VERIFY_FAIL` 만 내려준다. `NOTICE` 는 Phase 2 에 합류하지만
 * 값을 미리 알고 있어야 그때 파싱이 깨지지 않으므로 지금 정의해 둔다.
 */
enum class ThreadItemType(
    val value: String,
) {
    VERIFY_SUCCESS("VERIFY_SUCCESS"),
    VERIFY_FAIL("VERIFY_FAIL"),
    NOTICE("NOTICE"),
    ;

    /** 인증 판정 이벤트인가 — 공지와 달리 상세로 들어갈 대상이 없다. */
    val isVerification: Boolean
        get() = this == VERIFY_SUCCESS || this == VERIFY_FAIL

    companion object {
        fun fromValue(value: String?): ThreadItemType? = entries.find { it.value == value }
    }
}

/**
 * 방 스레드 피드 아이템 (명세 `items[]`).
 *
 * [at] 은 **노출 기준 시각이자 정렬축**이다. 실패는 판정 시각이 아니라 공유 가능 시각이 여기 담기므로
 * 발생일보다 늦게 흐른다 — 그래서 실패 문구는 [failDate] 를 써서 "○월 ○일 실패했어요" 처럼
 * **과거형으로 날짜를 명시**한다. 비난이 아니라 사실 전달이 목적이다.
 */
data class ThreadItem(
    val type: ThreadItemType,
    // 댓글 대상 ID — VERIFY_* 는 verificationId, NOTICE 는 noticeId
    val id: String,
    val user: RoomUser,
    // ISO datetime
    val at: String,
    // VERIFY_SUCCESS 만 — 이 성공으로 이어진 연속 일수
    val streak: Int?,
    // VERIFY_FAIL 만 — 실패 귀속일 (ISO date)
    val failDate: String?,
    // NOTICE 만 — 공지 제목
    val title: String?,
    val commentCount: Int,
)

/**
 * 방 스레드 피드 (명세: GET /challenges/{id}/threads). ACTIVE 멤버 전용.
 *
 * [pinnedNotice] 는 피드에 섞지 않고 최상단에 따로 둔다. Phase 1 에서는 항상 null 이다.
 * [nextCursor] 가 null 이면 마지막 페이지다.
 */
data class ChallengeThreads(
    val pinnedNotice: NoticeSummary?,
    val items: List<ThreadItem>,
    val nextCursor: String?,
)

/** 커서가 서버에서 해석되지 않는다 (명세 400 `CURSOR_INVALID`) — 첫 페이지부터 다시 받는다. */
class ThreadCursorInvalidException : Exception("피드를 처음부터 다시 불러올게요.")
