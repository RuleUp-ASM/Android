package com.ruleup.challenge.data.repository

import com.ruleup.challenge.data.api.ChallengeApi
import com.ruleup.challenge.data.dto.MemberDecisionRequest
import com.ruleup.challenge.data.dto.RecommendationRequest
import com.ruleup.challenge.data.dto.toDomain
import com.ruleup.challenge.data.dto.toRequest
import com.ruleup.challenge.domain.ChallengeRepository
import com.ruleup.challenge.domain.entity.Challenge
import com.ruleup.challenge.domain.entity.ChallengeDetail
import com.ruleup.challenge.domain.entity.ChallengeForm
import com.ruleup.challenge.domain.entity.ChallengeMembers
import com.ruleup.challenge.domain.entity.ChallengeRecommendation
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.MemberAction
import com.ruleup.challenge.domain.entity.MemberStatus
import com.ruleup.challenge.domain.entity.MemberStatusFilter
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.requireField
import com.ruleup.network.dto.throwOnError
import com.ruleup.network.image.ImageReader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ChallengeRepositoryImpl(
    private val api: ChallengeApi,
    private val imageReader: ImageReader,
) : ChallengeRepository {
    override suspend fun recommend(
        title: String,
        description: String?,
    ): ChallengeRecommendation =
        api
            .recommend(
                RecommendationRequest(
                    title = title,
                    description = description,
                ),
            ).getOrThrow()
            .toDomain()

    override suspend fun create(form: ChallengeForm): Challenge =
        api
            .create(form.toRequest())
            .getOrThrow()
            .toDomain()

    override suspend fun uploadImage(imageUri: String): String {
        val image = imageReader.read(imageUri)
        val content =
            MultiPartFormDataContent(
                formData {
                    append(
                        key = "image",
                        value = image.bytes,
                        headers =
                            Headers.build {
                                append(HttpHeaders.ContentType, image.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"challenge_image\"")
                            },
                    )
                },
            )
        return api
            .uploadImage(content)
            .getOrThrow()
            .imageUrl
            .requireField("imageUrl")
    }

    override suspend fun getChallenge(challengeId: String): ChallengeDetail =
        api
            .getChallenge(challengeId)
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

    override suspend fun join(challengeId: String): MemberStatus =
        api
            .join(challengeId)
            .getOrThrow()
            .toDomain()

    override suspend fun decideMember(
        challengeId: String,
        userId: String,
        action: MemberAction,
    ): MemberStatus =
        api
            .decideMember(
                challengeId = challengeId,
                userId = userId,
                request = MemberDecisionRequest(action = action.value),
            ).getOrThrow()
            .toDomain()

    override suspend fun getMembers(
        challengeId: String,
        status: MemberStatusFilter,
    ): ChallengeMembers =
        api
            .getMembers(
                challengeId = challengeId,
                status = status.value,
            ).getOrThrow()
            .toDomain()
}
