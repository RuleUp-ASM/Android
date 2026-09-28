package com.ruleup.challenge.domain.entity

import com.ruleup.domain.entity.user.User

/** 스레드 페이징. */
object ThreadPolicy {
    const val PAGE_SIZE = 20
    const val MAX_PAGE_SIZE = 50
}

/** 피드 아이템 종류. */
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

/** 방 스레드 피드 아이템. */
data class ThreadItem(
    val type: ThreadItemType,
    // 판정 ID (verificationId).
    val id: String,
    val user: User,
    // ISO datetime
    val at: String,
    // VERIFY_SUCCESS 만
    val streak: Int?,
    // VERIFY_FAIL 만
    val failDate: String?,
)

/** 방 스레드 피드. */
data class ChallengeThreads(
    val items: List<ThreadItem>,
    val nextCursor: String?,
)

/** 커서가 서버에서 해석되지 않는다 */
class ThreadCursorInvalidException : Exception("피드를 처음부터 다시 불러올게요.")
