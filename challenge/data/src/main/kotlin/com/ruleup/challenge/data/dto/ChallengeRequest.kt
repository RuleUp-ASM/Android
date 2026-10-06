package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** PATCH 본문 조립 전용 직렬화기. */
private val ChallengeJson = Json { explicitNulls = false }

// 초안 생성 (POST /challenges/draft)
@Serializable
data class DraftRequest(
    // 루틴 설명
    @SerialName("description")
    val description: String,
)

// 템플릿 초안 (POST /challenges/recommendation/by-template)
@Serializable
data class RecommendByTemplateRequest(
    @SerialName("templateId")
    val templateId: Long,
)

// 챌린지 생성 (POST /challenges)

/** 생성 요청. */
@Serializable
data class CreateChallengeRequest(
    @SerialName("draftId")
    val draftId: String,
    @SerialName("title")
    val title: String,
    @SerialName("description")
    val description: String,
    @SerialName("category")
    val category: String,
    @SerialName("mode")
    val mode: String,
    @SerialName("visibility")
    val visibility: String? = null,
    @SerialName("rankingVisible")
    val rankingVisible: Boolean? = null,
    @SerialName("capacity")
    val capacity: Int? = null,
    @SerialName("minTier")
    val minTier: String? = null,
    @SerialName("period")
    val period: PeriodRequest,
    @SerialName("weeklyCount")
    val weeklyCount: Int,
    @SerialName("params")
    val params: List<ParamEntryRequest>,
    @SerialName("verification")
    val verification: VerificationRequest,
    @SerialName("penalties")
    val penalties: PenaltiesRequest,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

/** 생성 요청 본문. */
internal fun CreateChallengeCommand.toRequestBody(): JsonObject {
    val body = ChallengeJson.encodeToJsonElement(toRequest()).jsonObject
    return if (mode.isGroup && capacity == null) JsonObject(body + ("capacity" to JsonNull)) else body
}

internal fun CreateChallengeCommand.toRequest(): CreateChallengeRequest =
    CreateChallengeRequest(
        draftId = draftId,
        title = title,
        description = description,
        category = category.value,
        mode = mode.value,
        visibility = visibility?.value,
        rankingVisible = rankingVisible,
        capacity = capacity,
        minTier = minTier?.value,
        period = period.toRequest(),
        weeklyCount = weeklyCount,
        params = params.map { it.toRequest() },
        verification = verification.toRequest(),
        penalties = PenaltiesRequest(watcher = watcherPenalty),
        imageUrl = imageUrl,
    )

// 챌린지 수정 (PATCH /challenges/{id})

/** 수정 요청 본문을 직접 조립한다. */
internal fun ChallengeUpdate.toRequestBody(): JsonObject =
    buildJsonObject {
        put("version", version)
        title?.let { put("title", it) }
        description?.let { put("description", it) }
        when {
            removeImage -> put("imageUrl", JsonNull)
            imageUrl != null -> put("imageUrl", imageUrl)
        }
        mode?.let { put("mode", it.value) }
        visibility?.let { put("visibility", it.value) }
        rankingVisible?.let { put("rankingVisible", it) }
        when {
            unlimitedCapacity -> put("capacity", JsonNull)
            capacity != null -> put("capacity", capacity)
        }
        minTier?.let { put("minTier", it.value) }
        period?.let { put("period", it.toUpdateJson()) }
        weeklyCount?.let { put("weeklyCount", it) }
        params?.let { entries -> put("params", ChallengeJson.encodeToJsonElement(entries.map { it.toRequest() })) }
        verification?.let { put("verification", ChallengeJson.encodeToJsonElement(it.toRequest())) }
        watcherPenalty?.let {
            put("penalties", buildJsonObject { put("watcher", JsonPrimitive(it)) })
        }
    }

/**
 * 수정 요청의 기간. 종료일이 없는 방은 응답의 null 을 빈 문자열로 접어 들고 있으므로,
 * 그대로 보내면 서버가 날짜 형식 오류(400)로 막는다 → 받은 그대로 null 로 되돌려 보낸다.
 */
internal fun ChallengePeriod.toUpdateJson(): JsonObject =
    buildJsonObject {
        put("start", start)
        if (end.isBlank()) put("end", JsonNull) else put("end", end)
    }
