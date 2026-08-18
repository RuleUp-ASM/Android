package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeThreads
import com.ruleup.challenge.domain.entity.ThreadItem
import com.ruleup.challenge.domain.entity.ThreadItemType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 방 스레드 피드 (GET /challenges/{id}/threads) ----------

@Serializable
data class ThreadItemResponse(
    @SerialName("type")
    val type: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("user")
    val user: RoomUserResponse? = null,
    // 노출 기준 시각 = 정렬축. VERIFY_FAIL 은 판정 시각이 아니라 공유 가능 시각이다
    @SerialName("at")
    val at: String? = null,
    @SerialName("streak")
    val streak: Int? = null,
    @SerialName("failDate")
    val failDate: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("commentCount")
    val commentCount: Int? = null,
)

// 앱이 모르는 type 은 null 을 돌려주고 호출부가 아이템 자체를 버린다 — 정체를 모르는 카드를
// 빈 껍데기로 그리는 것보다 낫다.
internal fun ThreadItemResponse.toDomainOrNull(): ThreadItem? {
    val itemType = ThreadItemType.fromValue(type) ?: return null
    return ThreadItem(
        type = itemType,
        id = id.orEmpty(),
        user = user.toDomain(),
        at = at.orEmpty(),
        streak = streak,
        failDate = failDate,
        title = title,
        commentCount = commentCount ?: 0,
    )
}

@Serializable
data class ThreadsResponse(
    // Phase 1 에서는 항상 null (공지는 Phase 2)
    @SerialName("pinnedNotice")
    val pinnedNotice: NoticeSummaryResponse? = null,
    @SerialName("items")
    val items: List<ThreadItemResponse>? = null,
    // null 이면 마지막 페이지
    @SerialName("nextCursor")
    val nextCursor: String? = null,
)

internal fun ThreadsResponse.toDomain(): ChallengeThreads =
    ChallengeThreads(
        pinnedNotice = pinnedNotice?.toDomain(),
        items = items.orEmpty().mapNotNull { it.toDomainOrNull() },
        nextCursor = nextCursor,
    )
