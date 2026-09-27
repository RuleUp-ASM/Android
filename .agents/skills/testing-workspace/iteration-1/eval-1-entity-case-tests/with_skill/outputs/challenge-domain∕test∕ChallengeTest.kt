package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `Challenge.kt` 의 enum 계약. 값 문자열은 서버와 주고받는 실제 계약이고, `fromValue` 는 data 매퍼가
 * 응답을 접는 유일한 입구다 — 값이 하나 어긋나면 요청이 400 으로 막히거나 응답이 조용히 기본값으로 접힌다.
 *
 * [CreateChallengeCommand] 의 불변식은 `CreateChallengeCommandTest` 가 이미 본다. 나머지 data class 는
 * 규칙 없는 값 묶음이라 여기서 다루지 않는다 — 미검증 이유는 `NOTES.md` 참고.
 */
class ChallengeModeTest {
    @Test
    fun `참여 형태는 SOLO·GROUP 두 종이고 서버 값과 이름이 같다`() {
        assertEquals(listOf("SOLO", "GROUP"), ChallengeMode.entries.map { it.value })
    }

    @Test
    fun `남과 함께하는 형태는 GROUP 하나뿐이다`() {
        // 방 홈·랭킹·멤버 관리가 열리는 조건이다. 형태가 늘면 여기서 "그것도 그룹인가"를 정하고 가야 한다.
        assertEquals(listOf(ChallengeMode.GROUP), ChallengeMode.entries.filter { it.isGroup })
    }

    @Test
    fun `모르는 참여 형태는 null 이다`() {
        // data 가 이 null 을 받아 기본값으로 접는다 — 여기서 던지면 목록 전체가 못 그려진다.
        assertNull(ChallengeMode.fromValue("PARTICIPATION"))
        assertNull(ChallengeMode.fromValue(null))
    }
}

class ChallengeVisibilityTest {
    @Test
    fun `공개 범위는 PUBLIC·PRIVATE 두 종이고 서버 값과 이름이 같다`() {
        assertEquals(listOf("PUBLIC", "PRIVATE"), ChallengeVisibility.entries.map { it.value })
    }

    @Test
    fun `초대 링크로만 들어오는 범위는 PRIVATE 하나뿐이다`() {
        // 탐색 노출과 참여 게이트가 이 하나로 갈린다.
        assertEquals(listOf(ChallengeVisibility.PRIVATE), ChallengeVisibility.entries.filter { it.isPrivate })
    }

    @Test
    fun `모르는 공개 범위는 null 이다`() {
        assertNull(ChallengeVisibility.fromValue("INVITE_ONLY"))
        assertNull(ChallengeVisibility.fromValue(null))
    }
}

class ChallengeStatusTest {
    @Test
    fun `생애주기는 UPCOMING·ACTIVE·COMPLETED 세 종이고 서버 값과 이름이 같다`() {
        // 기간 만료 후 자동 삭제는 배치 몫이라 DELETED 같은 상태는 계약에 없다 — 클라는 404 로만 안다.
        assertEquals(listOf("UPCOMING", "ACTIVE", "COMPLETED"), ChallengeStatus.entries.map { it.value })
    }

    @Test
    fun `모르는 상태는 null 이다`() {
        assertNull(ChallengeStatus.fromValue("DELETED"))
        assertNull(ChallengeStatus.fromValue(null))
    }
}

class ChallengeFieldTest {
    @Test
    fun `수정 폼 필드 13종이 서버 필드명과 하나씩 짝을 이룬다`() {
        // 이름은 상수라 컴파일러가 보지만 value 는 서버 필드명이라 오타가 그대로 나간다 — 잠금 목록
        // (editableFields) 과 수정 결과(updatedFields) 가 이 값으로 대조되므로, 어긋난 항목은
        // "수정 가능한데 잠긴 필드"가 되어 화면에서 사라진다.
        val expected =
            mapOf(
                "title" to ChallengeField.TITLE,
                "description" to ChallengeField.DESCRIPTION,
                "imageUrl" to ChallengeField.IMAGE_URL,
                "mode" to ChallengeField.MODE,
                "visibility" to ChallengeField.VISIBILITY,
                "rankingVisible" to ChallengeField.RANKING_VISIBLE,
                "capacity" to ChallengeField.CAPACITY,
                "minTier" to ChallengeField.MIN_TIER,
                "period" to ChallengeField.PERIOD,
                "weeklyCount" to ChallengeField.WEEKLY_COUNT,
                "params" to ChallengeField.PARAMS,
                "verification" to ChallengeField.VERIFICATION,
                "penalties" to ChallengeField.PENALTIES,
            )

        expected.forEach { (value, field) -> assertEquals(field, ChallengeField.fromValue(value)) }
        assertEquals(expected.size, ChallengeField.entries.size)
    }

    @Test
    fun `모르는 필드명은 null 이라 수정 가능 목록에서 조용히 빠진다`() {
        // data 가 mapNotNull 로 접는다 — 서버가 필드를 추가해도 설정 화면이 통째로 실패하지 않아야 한다.
        assertNull(ChallengeField.fromValue("titleEdited"))
        assertNull(ChallengeField.fromValue(null))
    }
}
