package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.RoomTopRanker
import com.ruleup.challenge.domain.entity.RoomUser
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 방 내부 조회 (GET /challenges/{id}/room) ----------

/** 스레드·랭킹이 공유하는 사람 표현. 서버가 마스킹을 마친 값이 온다. */
@Serializable
data class RoomUserResponse(
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("blocked")
    val blocked: Boolean? = null,
)

internal fun RoomUserResponse?.toDomain(): RoomUser =
    RoomUser(
        userId = this?.userId.orEmpty(),
        nickname = this?.nickname.orEmpty(),
        profileImageUrl = this?.profileImageUrl,
        blocked = this?.blocked ?: false,
    )

@Serializable
data class RoomSummaryResponse(
    @SerialName("title")
    val title: String? = null,
    // 방 전체 성공률 0~1. 판정 이력이 없으면 null 로 내려온다
    @SerialName("roomSuccessRate")
    val roomSuccessRate: Double? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
)

/** 방 홈 상위 3 랭킹. 전체 랭킹과 달리 user 오브젝트 없이 평평하게 내려온다. */
@Serializable
data class RoomTopRankerResponse(
    @SerialName("rank")
    val rank: Int? = null,
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("successRate")
    val successRate: Double? = null,
)

internal fun RoomTopRankerResponse.toDomain(): RoomTopRanker =
    RoomTopRanker(
        rank = rank ?: 0,
        userId = userId.orEmpty(),
        nickname = nickname.orEmpty(),
        profileImageUrl = profileImageUrl,
        successRate = successRate ?: 0.0,
    )

@Serializable
data class RoomResponse(
    @SerialName("myRole")
    val myRole: String? = null,
    @SerialName("ownerType")
    val ownerType: String? = null,
    @SerialName("summary")
    val summary: RoomSummaryResponse? = null,
    // 응답의 pinnedNotice 는 읽지 않는다 — 공지가 제품에서 빠졌다.
    @SerialName("topRanking")
    val topRanking: List<RoomTopRankerResponse>? = null,
    @SerialName("myTodayStatus")
    val myTodayStatus: String? = null,
)

internal fun RoomResponse.toDomain(): ChallengeRoom =
    ChallengeRoom(
        // 서버 합의: 미지 role 값은 MEMBER 취급 (운영 스프린트의 값 추가에 대비)
        myRole = MemberRole.fromValue(myRole) ?: MemberRole.MEMBER,
        ownerType = OwnerType.fromValue(ownerType),
        summary =
            RoomSummary(
                title = summary?.title.orEmpty(),
                // 표본 없음(null)을 0% 로 접지 않는다 — 갓 만든 방이 실패한 방처럼 보인다
                roomSuccessRate = summary?.roomSuccessRate,
                remainingDays = summary?.remainingDays ?: 0,
                participantCount = summary?.participantCount ?: 0,
                capacity = summary?.capacity ?: 0,
            ),
        topRanking = topRanking.orEmpty().map { it.toDomain() },
        // 미지 값은 null — 성공·실패 어느 쪽으로도 임의로 접지 않고 표기를 생략한다
        myTodayStatus = TodayVerificationStatus.fromValue(myTodayStatus),
    )
