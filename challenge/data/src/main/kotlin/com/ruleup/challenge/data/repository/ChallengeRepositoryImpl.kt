package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.DelegationActionRequest
import com.ruleup.challenge.data.dto.DelegationRequestBody
import com.ruleup.challenge.data.dto.DraftRequest
import com.ruleup.challenge.data.dto.MemberRoleActionRequest
import com.ruleup.challenge.data.dto.RecommendByTemplateRequest
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.data.dto.toRequest
import com.ruleup.challenge.data.dto.toRequestBody
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeNotEditableException
import com.ruleup.challenge.domain.entity.ChallengeNotFoundException
import com.ruleup.challenge.domain.entity.ChallengeSettings
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.ChallengeUpdateResult
import com.ruleup.challenge.domain.entity.ChallengeVersionConflictException
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.CreatedChallenge
import com.ruleup.challenge.domain.entity.DelegationAction
import com.ruleup.challenge.domain.entity.DelegationResolution
import com.ruleup.challenge.domain.entity.DelegationTicket
import com.ruleup.challenge.domain.entity.DeleteResult
import com.ruleup.challenge.domain.entity.DraftExpiredException
import com.ruleup.challenge.domain.entity.DraftResult
import com.ruleup.challenge.domain.entity.InvalidWeeklyCountException
import com.ruleup.challenge.domain.entity.JoinBlockReason
import com.ruleup.challenge.domain.entity.JoinBlockedException
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.LeaveResult
import com.ruleup.challenge.domain.entity.MemberRoleChange
import com.ruleup.challenge.domain.entity.ModerationLockedException
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.OwnerAlreadyExistsException
import com.ruleup.challenge.domain.entity.OwnerClaimResult
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.entity.RoleAction
import com.ruleup.challenge.domain.entity.RoutineDescription
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.image.ImageReader
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class ChallengeRepositoryImpl
    @Inject
    constructor(
        private val api: ChallengeApi,
        private val imageReader: ImageReader,
    ) : ChallengeRepository {
        override suspend fun getRoutineTemplates(): List<RoutineTemplate> =
            api
                .getRoutineTemplates()
                .getOrThrow()
                .toDomain()

        override suspend fun createDraft(description: RoutineDescription): DraftResult =
            try {
                api
                    .createDraft(DraftRequest(description = description.value))
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 429 는 화면이 카운트다운으로 버튼을 잠그도록 도메인 예외로 옮긴다. 자동 재시도는 금지.
                if (e.code == CODE_RATE_LIMITED) {
                    throw RecommendationRateLimitedException(retryAfterSeconds = e.retryAfterSeconds)
                }
                throw e
            }

        override suspend fun createDraftFromTemplate(templateId: Long): DraftResult.Ok =
            api
                .createDraftFromTemplate(RecommendByTemplateRequest(templateId = templateId))
                .getOrThrow()
                .toDomain()

        override suspend fun create(
            command: CreateChallengeCommand,
            idempotencyKey: String,
        ): CreatedChallenge =
            try {
                api
                    .create(idempotencyKey = idempotencyKey, request = command.toRequest())
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 초안은 24시간만 산다 — 화면이 "다시 만들어 주세요"로 안내하도록 구분해 올린다.
                if (e.code == CODE_DRAFT_NOT_FOUND || e.code == CODE_DRAFT_EXPIRED) {
                    throw DraftExpiredException()
                }
                if (e.code == CODE_INVALID_WEEKLY_COUNT) throw InvalidWeeklyCountException()
                throw e
            }

        override suspend fun uploadImage(imageUri: String): String {
            val image = imageReader.read(imageUri)
            val part =
                MultipartBody.Part.createFormData(
                    name = "image",
                    filename = "challenge_image",
                    body = image.bytes.toRequestBody(image.mimeType.toMediaType()),
                )
            return api
                .uploadImage(part)
                .getOrThrow()
                .imageUrl
                .requireField("imageUrl")
        }

        override suspend fun getChallenge(challengeId: String): ChallengeDetail =
            try {
                api
                    .getChallenge(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 없음·비공개·솔로를 구분하지 않는다(존재 은닉) — 화면 문구도 하나다.
                if (e.code == CODE_CHALLENGE_NOT_FOUND) throw ChallengeNotFoundException()
                throw e
            }

        override suspend fun getSetupInfo(challengeId: String): ChallengeSetupInfo =
            api
                .getSetup(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun getSettings(challengeId: String): ChallengeSettings =
            api
                .getSettings(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun update(
            challengeId: String,
            update: ChallengeUpdate,
        ): ChallengeUpdateResult =
            try {
                api
                    .update(challengeId, update.toRequestBody())
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                when (e.code) {
                    // 둘 다 "서버 기준으로 다시 그려라"로 귀결되지만, 문구가 달라 타입을 나눈다.
                    CODE_VERSION_CONFLICT -> throw ChallengeVersionConflictException()
                    CODE_NOT_EDITABLE -> throw ChallengeNotEditableException()
                    CODE_MODERATION_LOCKED -> throw ModerationLockedException(retryAfterSeconds = e.retryAfterSeconds)
                    CODE_INVALID_WEEKLY_COUNT -> throw InvalidWeeklyCountException()
                    else -> throw e
                }
            }

        override suspend fun delete(challengeId: String): DeleteResult =
            api
                .delete(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun join(challengeId: String): JoinResult =
            try {
                api
                    .join(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 거절은 전부 409 JOIN_BLOCKED + reason 단일 형식이다(구 403/409 분리 표기 폐기).
                if (e.code == CODE_JOIN_BLOCKED) {
                    throw JoinBlockedException(
                        reason = JoinBlockReason.fromValue(e.reason),
                        rejoinAvailableAt = e.rejoinAvailableAt,
                    )
                }
                if (e.code == CODE_CHALLENGE_NOT_FOUND) throw ChallengeNotFoundException()
                throw e
            }

        override suspend fun getMembers(challengeId: String): ChallengeMembers =
            api
                .getMembers(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun getMyChallenges(): List<MyChallenge> =
            api
                .getMyChallenges()
                .getOrThrow()
                .toDomain()

        override suspend fun leaveChallenge(challengeId: String): LeaveResult =
            api
                .leaveChallenge(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun changeMemberRole(
            challengeId: String,
            userId: String,
            action: RoleAction,
        ): MemberRoleChange =
            api
                .changeMemberRole(
                    challengeId = challengeId,
                    userId = userId,
                    request = MemberRoleActionRequest(action = action.value),
                ).getOrThrow()
                .toDomain()

        override suspend fun requestDelegation(
            challengeId: String,
            targetUserId: String,
        ): DelegationTicket =
            api
                .requestDelegation(
                    challengeId = challengeId,
                    request = DelegationRequestBody(targetUserId = targetUserId),
                ).getOrThrow()
                .toDomain()

        override suspend fun respondDelegation(
            challengeId: String,
            delegationId: String,
            action: DelegationAction,
        ): DelegationResolution =
            api
                .respondDelegation(
                    challengeId = challengeId,
                    delegationId = delegationId,
                    request = DelegationActionRequest(action = action.value),
                ).getOrThrow()
                .toDomain()

        override suspend fun claimOwner(challengeId: String): OwnerClaimResult =
            try {
                api
                    .claimOwner(challengeId)
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 선착순에서 밀린 건 오류가 아니라 정상 결과다 — 화면이 안내 문구로 분기하도록 타입을 나눈다.
                if (e.code == CODE_OWNER_ALREADY_EXISTS) throw OwnerAlreadyExistsException()
                throw e
            }

        private companion object {
            const val CODE_OWNER_ALREADY_EXISTS = "OWNER_ALREADY_EXISTS"
            const val CODE_RATE_LIMITED = "RECOMMENDATION_RATE_LIMITED"
            const val CODE_DRAFT_NOT_FOUND = "DRAFT_NOT_FOUND"
            const val CODE_DRAFT_EXPIRED = "DRAFT_EXPIRED"
            const val CODE_CHALLENGE_NOT_FOUND = "CHALLENGE_NOT_FOUND"
            const val CODE_VERSION_CONFLICT = "VERSION_CONFLICT"
            const val CODE_NOT_EDITABLE = "CHALLENGE_NOT_EDITABLE"
            const val CODE_MODERATION_LOCKED = "MODERATION_LOCKED"
            const val CODE_JOIN_BLOCKED = "JOIN_BLOCKED"
            const val CODE_INVALID_WEEKLY_COUNT = "INVALID_WEEKLY_COUNT"
        }
    }
