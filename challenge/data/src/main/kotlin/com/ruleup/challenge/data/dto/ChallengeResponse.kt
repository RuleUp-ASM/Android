package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.Anonymity
import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeEligibility
import com.ruleup.challenge.domain.entity.ChallengeMember
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MemberStatus
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.toRepeatDays
import com.ruleup.entity.user.InterestCategory
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// ---------- 3.1 LLM 기본값 추천 ----------
@Serializable
data class RecommendationResponse(
    @SerialName("matched")
    val matched: Boolean? = null,
    @SerialName("templateId")
    val templateId: Int? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("recommendedMethod")
    val recommendedMethod: String? = null,
    @SerialName("options")
    val options: List<VerificationOptionResponse>? = null,
    @SerialName("params")
    val params: List<ParamSpecResponse>? = null,
    @SerialName("rationale")
    val rationale: String? = null,
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
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
)

internal fun RecommendationResponse.toDomain(): ChallengeRecommendation =
    ChallengeRecommendation(
        matched = matched ?: false,
        templateId = templateId,
        title = title.requireField("title"),
        description = description,
        category = InterestCategory.fromValue(category.orEmpty()),
        recommendedMethod = SelectedMethod.fromValue(recommendedMethod) ?: SelectedMethod.MANUAL,
        options = options.orEmpty().map { it.toDomain() },
        params = params.orEmpty().map { it.toDomain() },
        rationale = rationale,
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        minMannerTemperature = minMannerTemperature,
        repeatDays = repeatDays.toRepeatDays(),
        durationDays = durationDays ?: 0,
        startDate = startDate.requireField("startDate"),
        endDate = endDate.requireField("endDate"),
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
    @SerialName("templateId")
    val templateId: Int? = null,
    @SerialName("verification")
    val verification: VerificationConfigResponse? = null,
    @SerialName("params")
    val params: JsonObject? = null,
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
        templateId = templateId,
        verification = verification?.toDomain(),
        params = params.toParamValueMap(),
        penalty = penalty.requireField("penalty").toDomain(),
        reward = reward.requireField("reward").toDomain(),
    )

// ---------- 3.3 챌린지 상세 + 참여 자격 ----------
@Serializable
data class ChallengeOwnerResponse(
    @SerialName("nickname")
    val nickname: String? = null,
)

@Serializable
data class ChallengeStatsResponse(
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("averageMannerTemperature")
    val averageMannerTemperature: Double? = null,
    @SerialName("completionRate")
    val completionRate: Double? = null,
)

@Serializable
data class ChallengeEligibilityResponse(
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
    val owner: ChallengeOwnerResponse? = null,
    @SerialName("repeatDays")
    val repeatDays: List<String>? = null,
    @SerialName("durationDays")
    val durationDays: Int? = null,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("templateId")
    val templateId: Int? = null,
    @SerialName("verification")
    val verification: VerificationConfigResponse? = null,
    @SerialName("params")
    val params: JsonObject? = null,
    @SerialName("penalty")
    val penalty: PenaltyDto? = null,
    @SerialName("reward")
    val reward: RewardDto? = null,
    @SerialName("stats")
    val stats: ChallengeStatsResponse? = null,
    @SerialName("eligibility")
    val eligibility: ChallengeEligibilityResponse? = null,
    // 요청자가 생성자인지 (감시자 관리 등 생성자 전용 UI 노출 판단)
    @SerialName("isOwner")
    val isOwner: Boolean? = null,
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
        templateId = templateId,
        verification = verification?.toDomain(),
        params = params.toParamValueMap(),
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
        isOwner = isOwner ?: false,
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
data class ChallengeMemberResponse(
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

internal fun ChallengeMemberResponse.toDomain(): ChallengeMember =
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
    val members: List<ChallengeMemberResponse>? = null,
)

internal fun ChallengeMembersResponse.toDomain(): ChallengeMembers =
    ChallengeMembers(
        challengeId = challengeId.requireField("challengeId"),
        participantCount = participantCount ?: 0,
        members = members?.map { it.toDomain() }.orEmpty(),
    )

// ---------- 3.9 챌린지 대표 이미지 업로드 ----------
@Serializable
data class ChallengeImageResponse(
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

// ---------- 내 챌린지 목록 조회 (GET /challenges) ----------
@Serializable
data class MyChallengeResponse(
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
    @SerialName("anonymity")
    val anonymity: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
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
    @SerialName("memberStatus")
    val memberStatus: String? = null,
)

internal fun MyChallengeResponse.toDomain(): MyChallenge =
    MyChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        description = description,
        imageUrl = imageUrl,
        category = InterestCategory.fromValue(category.orEmpty()),
        participationType = ParticipationType.fromValue(participationType) ?: ParticipationType.SOLO,
        status = ChallengeStatus.fromValue(status) ?: ChallengeStatus.RECRUITING,
        anonymity = Anonymity.fromValue(anonymity) ?: Anonymity.REAL,
        participantCount = participantCount ?: 0,
        minMannerTemperature = minMannerTemperature,
        repeatDays = repeatDays.toRepeatDays(),
        durationDays = durationDays ?: 0,
        startDate = startDate.requireField("startDate"),
        endDate = endDate.requireField("endDate"),
        memberStatus = MemberStatus.fromValue(memberStatus) ?: MemberStatus.ACTIVE,
    )

@Serializable
data class MyChallengesResponse(
    @SerialName("challenges")
    val challenges: List<MyChallengeResponse>? = null,
)

internal fun MyChallengesResponse.toDomain(): List<MyChallenge> = challenges.orEmpty().map { it.toDomain() }

// ---------- 챌린지 최초 조회 (GET setup) ----------
@Serializable
data class ChallengeSetupInfoResponse(
    @SerialName("setupStatus")
    val setupStatus: String? = null,
    @SerialName("manual")
    val manual: Boolean? = null,
    @SerialName("verificationMethods")
    val verificationMethods: List<String>? = null,
    @SerialName("requiredPermissions")
    val requiredPermissions: List<String>? = null,
    @SerialName("requiresAnchors")
    val requiresAnchors: Boolean? = null,
    @SerialName("anchorsConfigured")
    val anchorsConfigured: Boolean? = null,
    @SerialName("requiresTargetPackages")
    val requiresTargetPackages: Boolean? = null,
)

internal fun ChallengeSetupInfoResponse.toDomain(): ChallengeSetupInfo =
    ChallengeSetupInfo(
        manual = manual ?: false,
        requiredPermissions = requiredPermissions.orEmpty(),
        requiresAnchors = requiresAnchors ?: false,
        anchorsConfigured = anchorsConfigured ?: false,
        requiresTargetPackages = requiresTargetPackages ?: false,
    )
