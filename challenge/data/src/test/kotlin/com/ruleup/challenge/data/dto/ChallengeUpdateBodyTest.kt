package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengePeriod
import com.ruleup.challenge.domain.entity.ChallengeUpdate
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** 챌린지 수정 요청 본문. */
class ChallengeUpdateBodyTest {
    @Test
    fun `종료일이 없는 방의 시작일을 바꾸면 종료일을 빈 문자열이 아니라 null 로 보낸다`() {
        // 빈 문자열은 날짜 형식 오류라 서버가 400 으로 막는다(#564).
        val body = ChallengeUpdate(version = 1, period = ChallengePeriod(start = "2026-10-10", end = "")).toRequestBody()

        val period = body.getValue("period").jsonObject
        assertEquals(JsonPrimitive("2026-10-10"), period["start"])
        assertEquals(JsonNull, period["end"])
    }

    @Test
    fun `종료일이 있으면 그대로 보낸다`() {
        val body = ChallengeUpdate(version = 1, period = ChallengePeriod(start = "2026-10-10", end = "2026-11-10")).toRequestBody()

        assertEquals(JsonPrimitive("2026-11-10"), body.getValue("period").jsonObject["end"])
    }
}
