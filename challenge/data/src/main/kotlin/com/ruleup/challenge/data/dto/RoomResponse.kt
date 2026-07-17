package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeRoom
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 방 홈 일괄 조회 (GET /challenges/{id}/room) ----------
@Serializable
data class RoomSummaryResponse(
    @SerialName("title")
    val title: String? = null,
    // 그룹 완주율 (%)
    @SerialName("completionRate")
    val completionRate: Double? = null,
    @SerialName("avgMannerTemperature")
    val avgMannerTemperature: Double? = null,
    @SerialName("remainingDays")
    val remainingDays: Int? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
)

@Serializable
data class RoomResponse(
    @SerialName("myRole")
    val myRole: String? = null,
    @SerialName("summary")
    val summary: RoomSummaryResponse? = null,
    // 고정 공지 최대 1건 (preview 없이 내려온다)
    @SerialName("pinnedNotice")
    val pinnedNotice: NoticeSummaryResponse? = null,
    @SerialName("unreadNoticeCount")
    val unreadNoticeCount: Int? = null,
    @SerialName("topRanking")
    val topRanking: List<RankingEntryResponse>? = null,
    @SerialName("myTodayStatus")
    val myTodayStatus: String? = null,
)

internal fun RoomResponse.toDomain(): ChallengeRoom =
    ChallengeRoom(
        // 서버 합의: 미지 role 값은 MEMBER 취급 (운영 스프린트의 값 추가에 대비)
        myRole = MemberRole.fromValue(myRole) ?: MemberRole.MEMBER,
        summary =
            RoomSummary(
                title = summary?.title.orEmpty(),
                completionRate = summary?.completionRate ?: 0.0,
                avgMannerTemperature = summary?.avgMannerTemperature ?: 0.0,
                remainingDays = summary?.remainingDays ?: 0,
                participantCount = summary?.participantCount ?: 0,
            ),
        pinnedNotice = pinnedNotice?.toDomain(),
        unreadNoticeCount = unreadNoticeCount ?: 0,
        topRanking = topRanking.orEmpty().map { it.toDomain() },
        myTodayStatus = TodayVerificationStatus.fromValue(myTodayStatus) ?: TodayVerificationStatus.NOT_TARGET,
    )
