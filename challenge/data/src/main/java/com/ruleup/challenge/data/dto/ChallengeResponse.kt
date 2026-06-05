package com.ruleup.challenge.data.dto

import com.ruleup.entity.challenge.Challenge
import com.ruleup.entity.challenge.ChallengeDetail
import com.ruleup.entity.challenge.ChallengeEligibility
import com.ruleup.entity.challenge.ChallengeMember
import com.ruleup.entity.challenge.ChallengeMembers
import com.ruleup.entity.challenge.ChallengeOwner
import com.ruleup.entity.challenge.ChallengeRecommendation
import com.ruleup.entity.challenge.ChallengeStats
import com.ruleup.entity.challenge.ChallengeStatus
import com.ruleup.entity.challenge.MemberRole
import com.ruleup.entity.challenge.MemberStatus
import com.ruleup.entity.challenge.ParticipationType
import com.ruleup.entity.challenge.toRepeatDays
import com.ruleup.entity.challenge.toVerificationMethods
import com.ruleup.entity.user.InterestCategory
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 3.1 LLM 기본값 추천 ----------
@Serializable
data class RecommendationResponse(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("verificationMethods")
    val verificationMethods: List<String>? = null,
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
)

internal fun RecommendationResponse.toDomain(): ChallengeRecommendation =
    ChallengeRecommendation(
        title = title.requireField("title"),
        description = description,
        category = InterestCategory.fromValue(category.orEmpty()),
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        minMannerTemperature = minMannerTemperature,
        repeatDays = repeatDays.toRepeatDays(),
        durationDays = durationDays ?: 0,
        startDate = startDate.requireField("startDate"),
        endDate = endDate.requireField("endDate"),
        verificationMethods = verificationMethods.toVerificationMethods(),
        penalty = penalty.requireField("penalty").toDomain(),
        reward = reward.requireField("reward").toDomain(),
    )

// ---------- 3.2 / 3.4 챌린지 ----------
@Serializable
data class ChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("verificationMethods")
    val verificationMethods: List<String>? = null,
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
)

internal fun ChallengeResponse.toDomain(): Challenge =
    Challenge(
        challengeId = challengeId.requireField("challengeId"),
        status = ChallengeStatus.fromValue(status) ?: ChallengeStatus.RECRUITING,
        title = title.requireField("title"),
        description = description,
        imageUrl = imageUrl,
        category = InterestCategory.fromValue(category.orEmpty()),
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        minMannerTemperature = minMannerTemperature,
        repeatDays = repeatDays.toRepeatDays(),
        durationDays = durationDays ?: 0,
        startDate = startDate.requireField("startDate"),
        endDate = endDate.requireField("endDate"),
        verificationMethods = verificationMethods.toVerificationMethods(),
        penalty = penalty.requireField("penalty").toDomain(),
        reward = reward.requireField("reward").toDomain(),
    )

// ---------- 3.3 챌린지 상세 + 참여 자격 ----------
@Serializable
data class ChallengeOwnerDto(
    @SerialName("nickname")
    val nickname: String? = null,
)

@Serializable
data class ChallengeStatsDto(
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("averageMannerTemperature")
    val averageMannerTemperature: Double? = null,
    @SerialName("completionRate")
    val completionRate: Double? = null,
)

@Serializable
data class ChallengeEligibilityDto(
    @SerialName("canJoin")
    val canJoin: Boolean? = null,
    @SerialName("myMannerTemperature")
    val myMannerTemperature: Double? = null,
    @SerialName("minMannerTemperature")
    val minMannerTemperature: Double? = null,
)

@Serializable
data class ChallengeDetailResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("participationType")
    val participationType: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("owner")
    val owner: ChallengeOwnerDto? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("verificationMethods")
    val verificationMethods: List<String>? = null,
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
    @SerialName("stats")
    val stats: ChallengeStatsDto? = null,
    @SerialName("eligibility")
    val eligibility: ChallengeEligibilityDto? = null,
)

internal fun ChallengeDetailResponse.toDomain(): ChallengeDetail =
    ChallengeDetail(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        description = description,
        imageUrl = imageUrl,
        category = InterestCategory.fromValue(category.orEmpty()),
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        status = ChallengeStatus.fromValue(status) ?: ChallengeStatus.RECRUITING,
        owner =
            ChallengeOwner(
                nickname = owner?.nickname.requireField("owner.nickname"),
            ),
        repeatDays = repeatDays.toRepeatDays(),
        durationDays = durationDays ?: 0,
        startDate = startDate.requireField("startDate"),
        endDate = endDate.requireField("endDate"),
        verificationMethods = verificationMethods.toVerificationMethods(),
        penalty = penalty.requireField("penalty").toDomain(),
        reward = reward.requireField("reward").toDomain(),
        stats =
            ChallengeStats(
                participantCount = stats?.participantCount ?: 0,
                averageMannerTemperature = stats?.averageMannerTemperature,
                completionRate = stats?.completionRate,
            ),
        eligibility =
            ChallengeEligibility(
                canJoin = eligibility?.canJoin ?: false,
                myMannerTemperature = eligibility?.myMannerTemperature ?: 36.5,
                minMannerTemperature = eligibility?.minMannerTemperature,
            ),
    )

// ---------- 3.6 / 3.7 멤버 상태 ----------
@Serializable
data class MemberStatusResponse(
    @SerialName("memberStatus")
    val memberStatus: String? = null,
)

internal fun MemberStatusResponse.toDomain(): MemberStatus = MemberStatus.fromValue(memberStatus).requireField("memberStatus")

// ---------- 3.8 멤버 목록 ----------
@Serializable
data class ChallengeMemberDto(
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("role")
    val role: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("mannerTemperature")
    val mannerTemperature: Double? = null,
    @SerialName("joinedAt")
    val joinedAt: String? = null,
)

internal fun ChallengeMemberDto.toDomain(): ChallengeMember =
    ChallengeMember(
        userId = userId.requireField("userId"),
        nickname = nickname.requireField("nickname"),
        profileImageUrl = profileImageUrl,
        role = MemberRole.fromValue(role) ?: MemberRole.MEMBER,
        status = MemberStatus.fromValue(status) ?: MemberStatus.ACTIVE,
        mannerTemperature = mannerTemperature ?: 36.5,
        joinedAt = joinedAt.requireField("joinedAt"),
    )

@Serializable
data class ChallengeMembersResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("members")
    val members: List<ChallengeMemberDto>? = null,
)

internal fun ChallengeMembersResponse.toDomain(): ChallengeMembers =
    ChallengeMembers(
        challengeId = challengeId.requireField("challengeId"),
        participantCount = participantCount ?: 0,
        members = members?.map { it.toDomain() }.orEmpty(),
    )
