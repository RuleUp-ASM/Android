package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 서버 문자열과 짝이 맞는 enum 들. `fromValue` 는 응답을 읽는 유일한 관문이라, 값이 하나라도 어긋나면
 * 화면이 조용히 "모르는 값"으로 떨어진다 — 크래시가 아니라 빈칸으로 나타나므로 테스트로만 잡힌다.
 */
class ChallengeModeTest {
    @Test
    fun `명세의 2종을 모두 매핑한다`() {
        assertEquals(ChallengeMode.SOLO, ChallengeMode.fromValue("SOLO"))
        assertEquals(ChallengeMode.GROUP, ChallengeMode.fromValue("GROUP"))
        assertEquals(2, ChallengeMode.entries.size)
    }

    @Test
    fun `모르는 참여 형태는 null 이다`() {
        // 구 participationType 값이나 서버 신규 값이 와도 앱이 터지지 않아야 한다.
        assertNull(ChallengeMode.fromValue("PERSONAL"))
        assertNull(ChallengeMode.fromValue("solo"))
        assertNull(ChallengeMode.fromValue(null))
    }

    @Test
    fun `그룹만 isGroup 이다`() {
        // 방 홈·랭킹·멤버 관리가 이 한 줄로 열리고 닫힌다.
        assertTrue(ChallengeMode.GROUP.isGroup)
        assertFalse(ChallengeMode.SOLO.isGroup)
    }
}

class ChallengeVisibilityTest {
    @Test
    fun `명세의 2종을 모두 매핑한다`() {
        assertEquals(ChallengeVisibility.PUBLIC, ChallengeVisibility.fromValue("PUBLIC"))
        assertEquals(ChallengeVisibility.PRIVATE, ChallengeVisibility.fromValue("PRIVATE"))
        assertEquals(2, ChallengeVisibility.entries.size)
    }

    @Test
    fun `모르는 공개 범위는 null 이다`() {
        // 솔로 챌린지는 visibility 자체가 없어 null 로 내려온다 — 그 경로가 여기로 들어온다.
        assertNull(ChallengeVisibility.fromValue("SECRET"))
        assertNull(ChallengeVisibility.fromValue(null))
    }

    @Test
    fun `PRIVATE 만 isPrivate 이다`() {
        // 탐색 노출과 참여 게이트가 갈리는 지점이다.
        assertTrue(ChallengeVisibility.PRIVATE.isPrivate)
        assertFalse(ChallengeVisibility.PUBLIC.isPrivate)
    }
}

class ChallengeStatusTest {
    @Test
    fun `명세의 3종을 모두 매핑한다`() {
        assertEquals(ChallengeStatus.UPCOMING, ChallengeStatus.fromValue("UPCOMING"))
        assertEquals(ChallengeStatus.ACTIVE, ChallengeStatus.fromValue("ACTIVE"))
        assertEquals(ChallengeStatus.COMPLETED, ChallengeStatus.fromValue("COMPLETED"))
        assertEquals(3, ChallengeStatus.entries.size)
    }

    @Test
    fun `모르는 상태는 null 이다`() {
        // 만료 삭제는 배치가 하고 클라는 404 로만 인지하므로, DELETED 같은 상태는 계약에 없다.
        assertNull(ChallengeStatus.fromValue("DELETED"))
        assertNull(ChallengeStatus.fromValue(null))
    }
}

class ChallengeFieldTest {
    @Test
    fun `수정 폼이 다루는 13개 필드를 모두 매핑한다`() {
        // 서버 editableFields 가 이 이름으로 내려온다 — 하나라도 어긋나면 잠겨야 할 폼이 열린다.
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
    fun `모르는 필드는 null 이다`() {
        // 서버가 모르는 값을 보내면 조용히 버리므로, 읽는 쪽도 예외 없이 흘려보내야 한다.
        assertNull(ChallengeField.fromValue("weekly_count"))
        assertNull(ChallengeField.fromValue(null))
    }
}
