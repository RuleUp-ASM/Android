package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.CreateChallengeCommand
import com.ruleup.challenge.domain.entity.VerificationConfig
import com.ruleup.challenge.domain.entity.VerificationMethod
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.domain.entity.category.Category
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * 생성 요청 본문의 정원. 무제한은 null 이라, 공용 Json 처럼 null 키를 빼면 서버가 무제한인지 누락인지
 * 가를 수 없다.
 */
class CreateChallengeRequestBodyTest {
    @Test
    fun `그룹 무제한 정원은 capacity 를 null 로 명시해 보낸다`() {
        val body = group(capacity = null).toRequestBody()

        assertEquals(JsonNull, body["capacity"])
    }

    @Test
    fun `그룹 유한 정원은 숫자로 보낸다`() {
        assertEquals(JsonPrimitive(30), group(capacity = 30).toRequestBody()["capacity"])
    }

    @Test
    fun `솔로 생성 요청에는 capacity 키가 없다`() {
        // 솔로에 정원 키를 실으면 서버가 그룹 전용 필드로 막는다.
        assertFalse(solo().toRequestBody().containsKey("capacity"))
    }

    private fun group(capacity: Int?) =
        command(mode = ChallengeMode.GROUP, visibility = ChallengeVisibility.PUBLIC, rankingVisible = null, capacity = capacity)

    private fun solo() = command(mode = ChallengeMode.SOLO, visibility = null, rankingVisible = true, capacity = null)

    private fun command(
        mode: ChallengeMode,
        visibility: ChallengeVisibility?,
        rankingVisible: Boolean?,
        capacity: Int?,
    ) = CreateChallengeCommand(
        draftId = "d1",
        title = "아침 6시 기상",
        description = "매일 아침",
        category = Category.entries.first(),
        mode = mode,
        visibility = visibility,
        rankingVisible = rankingVisible,
        capacity = capacity,
        minTier = null,
        period = ChallengePeriod(start = "2026-09-01", end = "2026-09-15"),
        weeklyCount = 7,
        params = emptyList(),
        verification = VerificationConfig(type = VerificationType.MANUAL, method = VerificationMethod.entries.first()),
        watcherPenalty = false,
        imageUrl = null,
    )
}
