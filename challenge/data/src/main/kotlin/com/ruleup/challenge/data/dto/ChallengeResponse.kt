package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeConfig
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeField
import com.ruleup.challenge.domain.entity.ChallengeGate
import com.ruleup.challenge.domain.entity.ChallengeMember
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeModeration
import com.ruleup.challenge.domain.entity.ChallengeOwner
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeStats
import com.ruleup.challenge.domain.entity.ChallengeStatus
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DelegationResolution
import com.ruleup.challenge.domain.entity.DelegationStatus
import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.entity.DeleteResult
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinNote
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.domain.entity.MemberRoleChange
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.OwnerType
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 초안 생성 (POST /challenges/draft) ----------

/**
 * `result=FALLBACK` 이면 `draftId`·`draft` 가 null 이고 `message` 만 온다. **HTTP 200 이고 에러가 아니다.**
 */
@Serializable
data class DraftResponse(
    @SerialName("result")
    val result: String? = null,
    @SerialName("draftId")
    val draftId: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("draft")
    val draft: DraftDto? = null,
)

internal fun DraftResponse.toDomain(): DraftResult =
    // result 가 비어 오면 draftId 유무로 판별한다 — 폴백을 성공으로 오인해 빈 확인 화면을 띄우지 않기 위해서다.
    if (result == RESULT_FALLBACK || (result == null && draftId == null)) {
        DraftResult.Fallback(message = message?.takeIf { it.isNotBlank() } ?: FALLBACK_DEFAULT_MESSAGE)
    } else {
        DraftResult.Ok(
            draftId = draftId.requireField("draftId"),
            draft = draft.requireField("draft").toDomain(),
        )
    }

private const val RESULT_FALLBACK = "FALLBACK"
private const val FALLBACK_DEFAULT_MESSAGE = "루틴을 파악하지 못했어요. 조금 더 구체적으로 적어주세요."

// ---------- 템플릿 초안 (POST /challenges/recommendation/by-template) · 복제 (POST clone) ----------

/** 두 API 모두 `draftId` + 동일 스키마 `draft` 를 주며, 복제만 `sourceChallengeId` 를 덧붙인다. */
@Serializable
data class TemplateDraftResponse(
    @SerialName("draftId")
    val draftId: String? = null,
    @SerialName("sourceChallengeId")
    val sourceChallengeId: String? = null,
    @SerialName("draft")
    val draft: DraftDto? = null,
)

internal fun TemplateDraftResponse.toDomain(): DraftResult.Ok =
    DraftResult.Ok(
        draftId = draftId.requireField("draftId"),
        draft = draft.requireField("draft").toDomain(),
        sourceChallengeId = sourceChallengeId,
    )

// ---------- 추천 루틴 3개 (GET /challenges/recommendations) ----------
@Serializable
data class RoutineTemplateResponse(
    @SerialName("templateId")
    val templateId: Long? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("verificationType")
    val verificationType: String? = null,
    @SerialName("reason")
    val reason: String? = null,
)

@Serializable
data class RoutineTemplatesResponse(
    @SerialName("items")
    val items: List<RoutineTemplateResponse>? = null,
)

// templateId 가 없는 항목은 탭해도 by-template 을 부를 수 없어 무용하므로 버린다.
internal fun RoutineTemplatesResponse.toDomain(): List<RoutineTemplate> =
    items.orEmpty().mapNotNull { item ->
        val id = item.templateId ?: return@mapNotNull null
        RoutineTemplate(
            templateId = id,
            title = item.title.orEmpty(),
            description = item.description,
            category = Category.fromValue(item.category.orEmpty()),
            verificationType = VerificationType.fromValue(item.verificationType) ?: VerificationType.AUTO,
            reason = item.reason.orEmpty(),
        )
    }

// ---------- 챌린지 생성 (POST /challenges 201) ----------
@Serializable
data class CreateChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("moderation")
    val moderation: ModerationDto? = null,
    @SerialName("verification")
    val verification: VerificationDto? = null,
    @SerialName("personalSetupRequired")
    val personalSetupRequired: Boolean? = null,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

internal fun CreateChallengeResponse.toDomain(): CreatedChallenge =
    CreatedChallenge(
        challengeId = challengeId.requireField("challengeId"),
        status = ChallengeStatus.fromValue(status) ?: ChallengeStatus.UPCOMING,
        moderation = moderation?.toDomain() ?: NO_MODERATION,
        verification = verification.toDomain(),
        personalSetupRequired = personalSetupRequired ?: false,
        createdAt = createdAt.orEmpty(),
    )

private val NO_MODERATION =
    ChallengeModeration(
        title = ModerationState.NONE,
        description = ModerationState.NONE,
        image = ModerationState.NONE,
    )

// ---------- 방장 전용 설정 조회 (GET /challenges/{id}/settings) ----------
@Serializable
data class ChallengeConfigResponse(
    @SerialName("title")
    val title: String? = null,
    @SerialName("description")
    val description: String? = null,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
    @SerialName("category")
    val category: String? = null,
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("visibility")
    val visibility: String? = null,
    @SerialName("rankingVisible")
    val rankingVisible: Boolean? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("weeklyCount")
    val weeklyCount: Int? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("period")
    val period: PeriodDto? = null,
    @SerialName("params")
    val params: List<ParamSpecDto>? = null,
    @SerialName("verification")
    val verification: VerificationDto? = null,
    @SerialName("penalties")
    val penalties: PenaltiesDto? = null,
)

@Serializable
data class ChallengeSettingsResponse(
    @SerialName("config")
    val config: ChallengeConfigResponse? = null,
    @SerialName("editableFields")
    val editableFields: List<String>? = null,
    @SerialName("version")
    val version: Int? = null,
    @SerialName("moderation")
    val moderation: ModerationDto? = null,
)

internal fun ChallengeSettingsResponse.toDomain(): ChallengeSettings {
    val config = config.requireField("config")
    return ChallengeSettings(
        config =
            ChallengeConfig(
                title = config.title.orEmpty(),
                description = config.description.orEmpty(),
                imageUrl = config.imageUrl,
                category = Category.fromValue(config.category.orEmpty()),
                mode = ChallengeMode.fromValue(config.mode) ?: ChallengeMode.SOLO,
                visibility = config.visibility?.let(ChallengeVisibility::fromValue),
                rankingVisible = config.rankingVisible,
                capacity = config.capacity ?: 0,
                minTier = config.minTier?.let(Tier::fromValue),
                period = config.period.toDomain(),
                // 서버가 빠뜨리면 7(매일)로 본다 — 0 이면 "아무 날도 안 함"이 돼 화면이 거짓말을 한다.
                weeklyCount = (config.weeklyCount ?: DEFAULT_WEEKLY_COUNT).coerceIn(1, 7),
                params = config.params.orEmpty().map { it.toDomain() },
                verification = config.verification.toDomain(),
                penalties = config.penalties.toDomain(),
            ),
        // 모르는 필드명은 버린다 — 서버가 필드를 추가해도 구버전 앱이 폼을 잘못 여는 것보다 낫다.
        editableFields = editableFields.orEmpty().mapNotNull(ChallengeField::fromValue).toSet(),
        version = version.requireField("version"),
        moderation = moderation?.toDomain() ?: NO_MODERATION,
    )
}

// ---------- 챌린지 수정 (PATCH /challenges/{id}) ----------
@Serializable
data class UpdateChallengeResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("moderation")
    val moderation: ModerationDto? = null,
    // 반영된 필드 맵 — 키만 쓰고 값은 화면이 이미 들고 있다.
    @SerialName("updated")
    val updated: Map<String, kotlinx.serialization.json.JsonElement>? = null,
)

internal fun UpdateChallengeResponse.toDomain(): ChallengeUpdateResult =
    ChallengeUpdateResult(
        challengeId = challengeId.orEmpty(),
        moderation = moderation?.toDomain(),
        updatedFields =
            updated
                .orEmpty()
                .keys
                .mapNotNull(ChallengeField::fromValue)
                .toSet(),
    )

// ---------- 공개 상세 (GET /challenges/{challengeId}) ----------
@Serializable
data class ChallengeOwnerResponse(
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
)

@Serializable
data class ChallengeStatsResponse(
    @SerialName("completionRate")
    val completionRate: Double? = null,
    @SerialName("retentionRate")
    val retentionRate: Double? = null,
)

@Serializable
data class ChallengeGateResponse(
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("myDisplayTier")
    val myDisplayTier: String? = null,
    @SerialName("eligible")
    val eligible: Boolean? = null,
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
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("visibility")
    val visibility: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("owner")
    val owner: ChallengeOwnerResponse? = null,
    @SerialName("ownerType")
    val ownerType: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("isFull")
    val isFull: Boolean? = null,
    @SerialName("period")
    val period: PeriodDto? = null,
    @SerialName("verification")
    val verification: VerificationDto? = null,
    @SerialName("stats")
    val stats: ChallengeStatsResponse? = null,
    @SerialName("gate")
    val gate: ChallengeGateResponse? = null,
    @SerialName("joinBlockReason")
    val joinBlockReason: String? = null,
    @SerialName("rejoinAvailableAt")
    val rejoinAvailableAt: String? = null,
    @SerialName("joinNote")
    val joinNote: String? = null,
    @SerialName("cloneable")
    val cloneable: Boolean? = null,
    @SerialName("myRole")
    val myRole: String? = null,
    @SerialName("moderation")
    val moderation: ModerationDto? = null,
)

/**
 * 필수는 식별자뿐이다. 지표·자격이 하나 빠졌다고 상세를 실패시키면 서버가 필드를 늘릴 때 구버전 앱이 통째로
 * 막힌다. 다만 **완주율·유지율의 null 은 기본값으로 접지 않는다** — 표본 미달을 뜻하는 값이라 0으로 바꾸면
 * 거짓 정보가 된다.
 */
internal fun ChallengeDetailResponse.toDomain(): ChallengeDetail =
    ChallengeDetail(
        challengeId = challengeId.requireField("challengeId"),
        title = title.orEmpty(),
        description = description,
        imageUrl = imageUrl,
        category = Category.fromValue(category.orEmpty()),
        mode = ChallengeMode.fromValue(mode) ?: ChallengeMode.GROUP,
        visibility = visibility?.let(ChallengeVisibility::fromValue),
        status = ChallengeStatus.fromValue(status) ?: ChallengeStatus.ACTIVE,
        owner = owner?.let { ChallengeOwner(userId = it.userId.orEmpty(), nickname = it.nickname.orEmpty()) },
        ownerType = OwnerType.fromValue(ownerType),
        participantCount = participantCount ?: 0,
        capacity = capacity ?: 0,
        isFull = isFull ?: false,
        period = period.toDomain(),
        verification = verification.toDomain(),
        stats =
            ChallengeStats(
                completionRate = stats?.completionRate,
                retentionRate = stats?.retentionRate,
            ),
        gate =
            ChallengeGate(
                minTier = gate?.minTier?.let(Tier::fromValue),
                myDisplayTier = gate?.myDisplayTier?.let(Tier::fromValue),
                // 자격을 모르면 막는 쪽으로 떨어뜨린다 — 못 들어갈 방에 참여 버튼을 열어주면 안 된다.
                eligible = gate?.eligible ?: false,
            ),
        joinBlockReason = JoinBlockReason.fromValue(joinBlockReason),
        rejoinAvailableAt = rejoinAvailableAt,
        joinNote = JoinNote.fromValue(joinNote),
        cloneable = cloneable ?: false,
        myRole = MemberRole.fromValue(myRole) ?: MemberRole.NONE,
        moderation = moderation?.toDomain(),
    )

// ---------- 챌린지 삭제 (DELETE) ----------
@Serializable
data class DeleteChallengeResponse(
    @SerialName("penaltyApplied")
    val penaltyApplied: Boolean? = null,
)

internal fun DeleteChallengeResponse.toDomain(): DeleteResult = DeleteResult(penaltyApplied = penaltyApplied ?: false)

// ---------- 탈퇴 (DELETE members/me) ----------
@Serializable
data class LeaveChallengeResponse(
    @SerialName("penaltyApplied")
    val penaltyApplied: Boolean? = null,
)

internal fun LeaveChallengeResponse.toDomain(): LeaveResult = LeaveResult(penaltyApplied = penaltyApplied ?: false)

// ---------- 공동 관리자 임명/해제 (PATCH members/{userId}/role) ----------
@Serializable
data class MemberRoleResponse(
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("role")
    val role: String? = null,
)

internal fun MemberRoleResponse.toDomain(): MemberRoleChange =
    MemberRoleChange(
        userId = userId.requireField("userId"),
        role = MemberRole.fromValue(role) ?: MemberRole.MEMBER,
    )

// ---------- 방장 위임 (POST·PATCH delegation) ----------
@Serializable
data class DelegationResponse(
    @SerialName("delegationId")
    val delegationId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("expiresAt")
    val expiresAt: String? = null,
)

internal fun DelegationResponse.toDomain(): DelegationTicket =
    DelegationTicket(
        delegationId = delegationId.requireField("delegationId"),
        status = DelegationStatus.fromValue(status) ?: DelegationStatus.PENDING,
        expiresAt = expiresAt.orEmpty(),
    )

@Serializable
data class DelegationResolutionResponse(
    @SerialName("status")
    val status: String? = null,
    @SerialName("newOwnerUserId")
    val newOwnerUserId: String? = null,
)

internal fun DelegationResolutionResponse.toDomain(): DelegationResolution =
    DelegationResolution(
        status = DelegationStatus.fromValue(status) ?: DelegationStatus.PENDING,
        newOwnerUserId = newOwnerUserId,
    )

// ---------- 챌린지 가입 (POST members) ----------
@Serializable
data class JoinResponse(
    @SerialName("joined")
    val joined: Boolean? = null,
    @SerialName("countFromCycle")
    val countFromCycle: String? = null,
    @SerialName("requiredPermissions")
    val requiredPermissions: List<String>? = null,
    @SerialName("personalSetupRequired")
    val personalSetupRequired: Boolean? = null,
)

internal fun JoinResponse.toDomain(): JoinResult =
    JoinResult(
        countFromCycle = countFromCycle,
        requiredPermissions = requiredPermissions.orEmpty(),
        personalSetupRequired = personalSetupRequired ?: false,
    )

// ---------- 멤버 목록 (GET members) ----------
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
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("joinedAt")
    val joinedAt: String? = null,
)

internal fun ChallengeMemberResponse.toDomain(): ChallengeMember =
    ChallengeMember(
        userId = userId.requireField("userId"),
        nickname = nickname.requireField("nickname"),
        profileImageUrl = profileImageUrl,
        role = MemberRole.fromValue(role) ?: MemberRole.MEMBER,
        tier = tier?.let(Tier::fromValue),
        joinedAt = joinedAt.requireField("joinedAt"),
    )

@Serializable
data class ChallengeMembersResponse(
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("members")
    val members: List<ChallengeMemberResponse>? = null,
)

internal fun ChallengeMembersResponse.toDomain(): ChallengeMembers =
    ChallengeMembers(
        challengeId = challengeId.requireField("challengeId"),
        participantCount = participantCount ?: 0,
        capacity = capacity ?: 0,
        members = members?.map { it.toDomain() }.orEmpty(),
    )

// ---------- 챌린지 대표 이미지 업로드 ----------
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
    @SerialName("mode")
    val mode: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("participantCount")
    val participantCount: Int? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("period")
    val period: PeriodDto? = null,
    @SerialName("myRole")
    val myRole: String? = null,
)

internal fun MyChallengeResponse.toDomain(): MyChallenge =
    MyChallenge(
        challengeId = challengeId.requireField("challengeId"),
        title = title.requireField("title"),
        description = description,
        imageUrl = imageUrl,
        category = Category.fromValue(category.orEmpty()),
        mode = ChallengeMode.fromValue(mode) ?: ChallengeMode.SOLO,
        status = ChallengeStatus.fromValue(status) ?: ChallengeStatus.UPCOMING,
        participantCount = participantCount ?: 0,
        capacity = capacity ?: 0,
        minTier = minTier?.let(Tier::fromValue),
        period = period.toDomain(),
        myRole = MemberRole.fromValue(myRole) ?: MemberRole.MEMBER,
    )

@Serializable
data class MyChallengesResponse(
    @SerialName("challenges")
    val challenges: List<MyChallengeResponse>? = null,
    // 서버가 items 로 바꿔도 견디게 둘 다 받는다 — 계약이 "수정중"이라 흔들릴 여지가 있다.
    @SerialName("items")
    val items: List<MyChallengeResponse>? = null,
)

internal fun MyChallengesResponse.toDomain(): List<MyChallenge> = (challenges ?: items).orEmpty().map { it.toDomain() }

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
