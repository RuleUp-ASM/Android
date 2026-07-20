package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.RecommendationRequest
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.data.dto.toRequest
import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengePermissionRequiredException
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.ChallengeSetupInfo
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.JoinResult
import com.ruleup.challenge.domain.entity.MyChallenge
import com.ruleup.challenge.domain.entity.RecommendationRateLimitedException
import com.ruleup.challenge.domain.repository.ChallengeRepository
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
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
        override suspend fun recommend(
            title: String,
            description: String?,
        ): ChallengeRecommendation =
            try {
                api
                    .recommend(
                        RecommendationRequest(
                            title = title,
                            description = description,
                        ),
                    ).getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // 429 rate limit 은 화면이 최초 생성 화면 복귀 + 재시도 안내로 분기하도록 도메인 예외로 변환.
                if (e.code == "RECOMMENDATION_RATE_LIMITED") {
                    throw RecommendationRateLimitedException(retryAfterSeconds = e.retryAfterSeconds)
                }
                throw e
            }

        override suspend fun create(form: ChallengeForm): Challenge =
            try {
                api
                    .create(form.toRequest())
                    .getOrThrow()
                    .toDomain()
            } catch (e: ApiException) {
                // AUTO 권한 부족 바운스는 프레젠테이션이 권한 요청·재시도할 수 있도록 도메인 예외로 변환한다.
                // TODO(server-contract): 400 바디가 부족 권한 목록을 주면 파싱해 requiredPermissions 로 전달.
                if (e.code == "ROUTINE_PERMISSION_REQUIRED") {
                    throw ChallengePermissionRequiredException()
                }
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
            api
                .getChallenge(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun getSetupInfo(challengeId: String): ChallengeSetupInfo =
            api
                .getSetup(challengeId)
                .getOrThrow()
                .toDomain()

        override suspend fun update(
            challengeId: String,
            update: ChallengeUpdate,
        ): Challenge =
            api
                .update(challengeId, update.toRequest())
                .getOrThrow()
                .toDomain()

        override suspend fun delete(challengeId: String) {
            api.delete(challengeId).throwOnError()
        }

        override suspend fun join(challengeId: String): JoinResult =
            api
                .join(challengeId)
                .getOrThrow()
                .toDomain()

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
    }
