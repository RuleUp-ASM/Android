package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.RoomTopRanker
import com.ruleup.challenge.domain.entity.RoutineProgress
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.entity.user.UserRelationship
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 방 내부 조회 (GET /challenges/{id}/room)

/** 스레드·랭킹이 공유하는 사람 표현. */
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

internal fun RoomUserResponse?.toDomain(): User =
    User(
        id = this?.userId.orEmpty(),
        nickname = if (this?.blocked == true) "차단한 사용자" else this?.nickname.orEmpty(),
        profileImageUrl = if (this?.blocked == true) null else this?.profileImageUrl,
        relationship = UserRelationship(blocked = this?.blocked ?: false),
    )

@Serializable
data class RoomSummaryResponse(
    @SerialName("title")
    val title: String? = null,
    // 방 전체 성공률 0~1.
    @SerialName("roomSuccessRate")
    val roomSuccessRate: Double? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
)

/** 방 홈 상위 3 랭킹. */
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

/** 상위 3 에는 등재자만 올라온다(참여 10회 이상) */
internal fun RoomTopRankerResponse.toDomain(): RoomTopRanker? {
    val rank = rank ?: return null
    val successRate = successRate ?: return null
    val userId = userId ?: return null
    return RoomTopRanker(
        rank = rank,
        user = User(userId, nickname.orEmpty(), profileImageUrl),
        successRate = successRate,
    )
}

@Serializable
data class RoomResponse(
    @SerialName("myRole")
    val myRole: String? = null,
    @SerialName("ownerType")
    val ownerType: String? = null,
    @SerialName("summary")
    val summary: RoomSummaryResponse? = null,
    // 응답의 pinnedNotice 는 읽지 않는다
    @SerialName("topRanking")
    val topRanking: List<RoomTopRankerResponse>? = null,
    @SerialName("myTodayStatus")
    val myTodayStatus: String? = null,
    @SerialName("routineProgress")
    val routineProgress: RoomRoutineProgressResponse? = null,
)

internal fun RoomResponse.toDomain(): ChallengeRoom =
    ChallengeRoom(
        // 서버 합의: 미지 role 값은 MEMBER 취급 (운영 스프린트의 값 추가에 대비)
        myRole = MemberRole.fromValue(myRole) ?: MemberRole.MEMBER,
        ownerType = OwnerType.fromValue(ownerType),
        summary =
            RoomSummary(
                title = summary?.title.orEmpty(),
                // 표본 없음(null)을 0% 로 접지 않는다
                roomSuccessRate = summary?.roomSuccessRate,
                remainingDays = summary?.remainingDays ?: 0,
                participantCount = summary?.participantCount ?: 0,
                capacity = summary?.capacity,
            ),
        topRanking = topRanking.orEmpty().mapNotNull { it.toDomain() },
        // 미지 값은 null
        myTodayStatus = TodayVerificationStatus.fromValue(myTodayStatus),
        routineProgress = routineProgress?.toDomain(),
    )

/** 루틴 진행률. 비율은 %(0~100) 로 온다. roomAverageProgressRate(멤버 진행률 평균)는 화면에 없어 읽지 않는다 — summary.roomSuccessRate 와 다른 값이다. */
@Serializable
data class RoomRoutineProgressResponse(
    @SerialName("myProgressRate")
    val myProgressRate: Double? = null,
    @SerialName("mySuccessDays")
    val mySuccessDays: Int? = null,
    @SerialName("myTargetDays")
    val myTargetDays: Int? = null,
)

internal fun RoomRoutineProgressResponse.toDomain(): RoutineProgress =
    RoutineProgress(
        myProgressRate = (myProgressRate ?: 0.0) / 100,
        mySuccessDays = mySuccessDays ?: 0,
        myTargetDays = myTargetDays ?: 0,
    )
