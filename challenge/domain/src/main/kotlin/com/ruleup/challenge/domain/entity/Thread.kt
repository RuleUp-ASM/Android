package com.ruleup.challenge.domain.entity

/** 스레드 페이징 (명세 `size` — 기본 20 · 최대 50). */
object ThreadPolicy {
    const val PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50
}

/**
 * 피드 아이템 종류 (명세 `items[].type`).
 *
 * 피드는 **인증 판정만** 흐른다. 공지가 제품에서 빠지면서 `NOTICE` 는 앱이 아는 값이 아니게 됐고,
 * 서버가 보내더라도 [fromValue] 가 null 을 돌려 아이템이 조용히 버려진다 — 정체를 모르는 카드를
 * 빈 껍데기로 그리는 것보다 낫다.
 */
enum class ThreadItemType(
    val value: String,
) {
    VERIFY_SUCCESS("VERIFY_SUCCESS"),
    VERIFY_FAIL("VERIFY_FAIL"),
    ;

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
    // 판정 ID (verificationId). 목록 키로 쓴다
    val id: String,
    val user: RoomUser,
    // ISO datetime
    val at: String,
    // VERIFY_SUCCESS 만 — 이 성공으로 이어진 연속 일수
    val streak: Int?,
    // VERIFY_FAIL 만 — 실패 귀속일 (ISO date)
    val failDate: String?,
)

/**
 * 방 스레드 피드 (명세: GET /challenges/{id}/threads). ACTIVE 멤버 전용.
 *
 * 응답의 `pinnedNotice` 는 읽지 않는다 — 공지가 제품에서 빠졌다.
 * [nextCursor] 가 null 이면 마지막 페이지다.
 */
data class ChallengeThreads(
    val items: List<ThreadItem>,
    val nextCursor: String?,
)

/** 커서가 서버에서 해석되지 않는다 (명세 400 `CURSOR_INVALID`) — 첫 페이지부터 다시 받는다. */
class ThreadCursorInvalidException : Exception("피드를 처음부터 다시 불러올게요.")
