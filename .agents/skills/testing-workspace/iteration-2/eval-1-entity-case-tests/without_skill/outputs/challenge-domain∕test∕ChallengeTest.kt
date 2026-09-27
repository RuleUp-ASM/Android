package com.ruleup.challenge.domain.entity

import com.ruleup.challenge.domain.fake.command
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `Challenge.kt` 의 enum 들은 **서버 문자열과 앱 타입 사이의 유일한 다리**다. data 레이어가
 * `fromValue(...) ?: 기본값` 으로 받으므로, 값이 하나 어긋나도 예외 없이 조용히 기본값으로 떨어진다 —
 * SOLO 방이 GROUP 으로 보이거나 진행 중인 방이 UPCOMING 으로 보이는 식이다.
 * 그래서 문자열 목록 자체를 고정한다.
 *
 * [CreateChallengeCommand] 의 조합 규칙은 `CreateChallengeCommandTest` 가 맡는다.
 */
class ChallengeModeTest {
    @Test
    fun `명세의 2종만 정의돼 있다`() {
        // 구 participationType 의 값(PERSONAL 등)이 남아 있으면 서버가 알아듣지 못한다.
        assertEquals(listOf("SOLO", "GROUP"), ChallengeMode.entries.map { it.value })
    }

    @Test
    fun `모든 값이 자기 문자열로 되돌아온다`() {
        ChallengeMode.entries.forEach { assertEquals(it, ChallengeMode.fromValue(it.value)) }
    }

    @Test
    fun `모르는 값은 null 이다`() {
        // data 가 이 null 을 보고 기본값으로 떨어뜨린다 — 앱이 터지지 않는 것이 계약이다.
        assertNull(ChallengeMode.fromValue("PERSONAL"))
        assertNull(ChallengeMode.fromValue("solo"))
        assertNull(ChallengeMode.fromValue(null))
    }

    @Test
    fun `GROUP 만 함께하는 챌린지다`() {
        // 방 홈·랭킹·멤버 관리가 이 하나로 열린다. entries 로 훑어 새 값이 늘어도 판정이 빠지지 않게 한다.
        assertEquals(listOf(ChallengeMode.GROUP), ChallengeMode.entries.filter { it.isGroup })
    }
}

class ChallengeVisibilityTest {
    @Test
    fun `명세의 2종만 정의돼 있다`() {
        assertEquals(listOf("PUBLIC", "PRIVATE"), ChallengeVisibility.entries.map { it.value })
    }

    @Test
    fun `모든 값이 자기 문자열로 되돌아온다`() {
        ChallengeVisibility.entries.forEach { assertEquals(it, ChallengeVisibility.fromValue(it.value)) }
    }

    @Test
    fun `모르는 값은 null 이다`() {
        // 솔로 챌린지는 visibility 자체가 없어 null 로 내려온다 — 그 경로도 여기로 들어온다.
        assertNull(ChallengeVisibility.fromValue("INVITE_ONLY"))
        assertNull(ChallengeVisibility.fromValue(null))
    }

    @Test
    fun `PRIVATE 만 초대 전용이다`() {
        // 탐색 노출과 참여 게이트가 이 판정으로 갈린다.
        assertEquals(listOf(ChallengeVisibility.PRIVATE), ChallengeVisibility.entries.filter { it.isPrivate })
    }
}

class ChallengeStatusTest {
    @Test
    fun `명세의 3종만 정의돼 있다`() {
        // 기간 만료 후 삭제는 배치가 처리하므로 DELETED 같은 상태는 계약에 없다(클라는 404 로만 인지한다).
        assertEquals(listOf("UPCOMING", "ACTIVE", "COMPLETED"), ChallengeStatus.entries.map { it.value })
    }

    @Test
    fun `모든 값이 자기 문자열로 되돌아온다`() {
        ChallengeStatus.entries.forEach { assertEquals(it, ChallengeStatus.fromValue(it.value)) }
    }

    @Test
    fun `모르는 값은 null 이다`() {
        assertNull(ChallengeStatus.fromValue("DELETED"))
        assertNull(ChallengeStatus.fromValue(null))
    }
}

class ChallengeFieldTest {
    @Test
    fun `수정 폼 필드 13종이 명세의 JSON 키와 1대1로 붙는다`() {
        // 서버는 모르는 키를 400 이 아니라 **조용히 버린다**. 그래서 오타가 나면 "수정했는데 안 바뀐다"로만
        // 드러난다 — 이 목록이 그 침묵을 막는 유일한 지점이다.
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
        // snake_case 로 오는 서버가 있으면 여기서 잡힌다. 서버가 필드를 추가해도 폼이 터지지 않아야 한다.
        assertNull(ChallengeField.fromValue("weekly_count"))
        assertNull(ChallengeField.fromValue("titleEdited"))
        assertNull(ChallengeField.fromValue(null))
    }
}

/**
 * 범위 숫자는 화면 위젯과 [CreateChallengeCommand] 검증이 **같은 상수**를 본다. 상수가 바뀌면
 * 서버(400 INVALID_WEEKLY_COUNT)와 어긋나므로, 값 자체를 명세로 못 박는다.
 *
 * `CreateChallengeCommandTest` 는 범위를 벗어난 쪽만 확인한다 — 여기서는 경계값이 **통과하는지**를 본다.
 * off-by-one 은 거절 케이스만으로는 드러나지 않는다.
 */
class ChallengeLimitsTest {
    @Test
    fun `명세가 정한 범위 숫자다`() {
        assertEquals(1, ChallengeLimits.WEEKLY_COUNT_MIN)
        assertEquals(7, ChallengeLimits.WEEKLY_COUNT_MAX)
        assertEquals(1, ChallengeLimits.CAPACITY_MIN)
        assertEquals(10_000, ChallengeLimits.CAPACITY_MAX)
    }

    @Test
    fun `주간 횟수 경계값은 통과한다`() {
        assertEquals(
            ChallengeLimits.WEEKLY_COUNT_MIN,
            command().copy(weeklyCount = ChallengeLimits.WEEKLY_COUNT_MIN).weeklyCount,
        )
        assertEquals(
            ChallengeLimits.WEEKLY_COUNT_MAX,
            command().copy(weeklyCount = ChallengeLimits.WEEKLY_COUNT_MAX).weeklyCount,
        )
    }

    @Test
    fun `그룹 정원 경계값은 통과한다`() {
        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = ChallengeLimits.CAPACITY_MIN,
            )

        assertEquals(ChallengeLimits.CAPACITY_MIN, group.capacity)
        assertEquals(ChallengeLimits.CAPACITY_MAX, group.copy(capacity = ChallengeLimits.CAPACITY_MAX).capacity)
    }
}
