package com.ruleup.challenge.data.dto

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
import kotlinx.serialization.json.put

/**
 * PATCH 본문 조립 전용 직렬화기. `explicitNulls = false` 라 값 없는 필드가 `null` 로 새어 나가지 않는다 —
 * 부분 수정에서 null 은 "기본 이미지로 되돌리기"라는 별도 의미를 갖기 때문에 실수로 실리면 안 된다.
 */
private val ChallengeJson = Json { explicitNulls = false }

// ---------- 초안 생성 (POST /challenges/draft) ----------
@Serializable
data class DraftRequest(
    // 루틴 설명 — 유일한 입력, 1~200자
    @SerialName("description")
    val description: String,
)

// ---------- 템플릿 초안 (POST /challenges/recommendation/by-template) ----------
@Serializable
data class RecommendByTemplateRequest(
    @SerialName("templateId")
    val templateId: Long,
)

// ---------- 챌린지 생성 (POST /challenges) ----------

/**
 * 생성 요청.
 *
 * **수정 여부 필드가 없다** — 심사 대상 판정은 서버가 [draftId] 로 보관 중인 원본 초안과 대조해서 한다.
 * 구 `titleEdited`·`descriptionEdited`·`aiTitle`·`sourceChallengeId` 요청 필드는 폐기됐다(변조 클라이언트가
 * 악성 제목을 미수정으로 위장해 심사를 우회하던 경로).
 *
 * `penalties` 에는 `watcher` 만 싣는다 — score·groupShare 는 서버가 강제한다.
 */
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
    val period: PeriodDto,
    @SerialName("weeklyCount")
    val weeklyCount: Int,
    @SerialName("params")
    val params: List<ParamEntryDto>,
    @SerialName("verification")
    val verification: VerificationDto,
    @SerialName("penalties")
    val penalties: PenaltiesDto,
    @SerialName("imageUrl")
    val imageUrl: String? = null,
)

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
        period = period.toDto(),
        weeklyCount = weeklyCount,
        params = params.map { it.toDto() },
        verification = verification.toDto(),
        penalties = PenaltiesDto(watcher = watcherPenalty),
        imageUrl = imageUrl,
    )

// ---------- 챌린지 수정 (PATCH /challenges/{id}) ----------

/**
 * 수정 요청 본문을 직접 조립한다.
 *
 * 명세가 **"미포함 = 미변경"과 "null = 기본 이미지로 되돌리기"를 구분**하라고 요구하는데, 데이터 클래스로는
 * 둘을 표현할 수 없다(nullable 필드 하나로는 "안 보냄"과 "null 보냄"이 같은 모양이 된다). 그래서 넣을 키만
 * 담은 [JsonObject] 를 만든다 — `imageUrl` 만 [JsonNull] 을 실을 수 있고, 나머지에 null 을 보내면 서버가
 * 400 `INVALID_FIELD_VALUE` 로 막는다.
 *
 * 파생 필드(`mode` 변경에 따른 `visibility`·`rankingVisible`·`penalties.groupShare` 정규화)는 **서버 책임**이라
 * 클라가 맞춰 보내지 않는다.
 */
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
        capacity?.let { put("capacity", it) }
        minTier?.let { put("minTier", it.value) }
        period?.let { put("period", ChallengeJson.encodeToJsonElement(it.toDto())) }
        weeklyCount?.let { put("weeklyCount", it) }
        params?.let { entries -> put("params", ChallengeJson.encodeToJsonElement(entries.map { it.toDto() })) }
        verification?.let { put("verification", ChallengeJson.encodeToJsonElement(it.toDto())) }
        watcherPenalty?.let {
            put("penalties", buildJsonObject { put("watcher", JsonPrimitive(it)) })
        }
    }

// ---------- 공동 관리자 임명/해제 (PATCH members/{userId}/role) ----------
@Serializable
data class MemberRoleActionRequest(
    @SerialName("action")
    val action: String,
)

// ---------- 방장 위임 (POST·PATCH delegation) ----------
@Serializable
data class DelegationRequestBody(
    @SerialName("targetUserId")
    val targetUserId: String,
)

@Serializable
data class DelegationActionRequest(
    @SerialName("action")
    val action: String,
)
