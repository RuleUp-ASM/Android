package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `Challenge.kt` 의 enum 들이 서버 문자열과 맺은 계약. 여기 걸리는 건 서버가 값을 늘렸거나 우리가 철자를
 * 틀렸다는 뜻이고, 두 경우 모두 화면에서는 "빈 값"으로만 보여 원인을 찾기 어렵다 — 매핑 지점에서 잡는다.
 *
 * 불변식을 가진 [CreateChallengeCommand] 는 `CreateChallengeCommandTest`, `ModerationState` 의 문자열
 * 매핑은 data 층(`ChallengeCommonDto`)에 있어 각각 그쪽 소관이다.
 */
class ChallengeModeTest {
    @Test
    fun `명세의 2종을 모두 매핑한다`() {
        val expected =
            mapOf(
                "SOLO" to ChallengeMode.SOLO,
                "GROUP" to ChallengeMode.GROUP,
            )

        expected.forEach { (value, mode) -> assertEquals(mode, ChallengeMode.fromValue(value)) }
        assertEquals(expected.size, ChallengeMode.entries.size)
    }

    @Test
    fun `모르는 참여 형태는 null 이다`() {
        // 폐기된 participationType 값이 남아 있어도 앱이 터지지 않고 미상으로 떨어져야 한다.
        assertNull(ChallengeMode.fromValue("PARTICIPATION"))
        assertNull(ChallengeMode.fromValue(null))
    }

    @Test
    fun `함께하는 챌린지는 그룹뿐이다`() {
        // 방 홈·랭킹·멤버 관리가 이 한 줄로 열리고 닫힌다. 참여 형태가 늘면 여기서 먼저 깨져야 한다.
        assertEquals(listOf(ChallengeMode.GROUP), ChallengeMode.entries.filter { it.isGroup })
    }
}

class ChallengeVisibilityTest {
    @Test
    fun `명세의 2종을 모두 매핑한다`() {
        val expected =
            mapOf(
                "PUBLIC" to ChallengeVisibility.PUBLIC,
                "PRIVATE" to ChallengeVisibility.PRIVATE,
            )

        expected.forEach { (value, visibility) -> assertEquals(visibility, ChallengeVisibility.fromValue(value)) }
        assertEquals(expected.size, ChallengeVisibility.entries.size)
    }

    @Test
    fun `모르는 공개 범위는 null 이다`() {
        // 솔로 챌린지는 아예 값을 안 준다 — null 이 정상 입력이라 예외가 아니라 null 로 받아야 한다.
        assertNull(ChallengeVisibility.fromValue("INVITE_ONLY"))
        assertNull(ChallengeVisibility.fromValue(null))
    }

    @Test
    fun `초대 링크로만 들어올 수 있는 건 비공개뿐이다`() {
        // 탐색 노출과 참여 게이트가 이 값으로 갈린다. 공개가 비공개로 읽히면 방이 탐색에서 사라진다.
        assertEquals(listOf(ChallengeVisibility.PRIVATE), ChallengeVisibility.entries.filter { it.isPrivate })
    }
}

class ChallengeStatusTest {
    @Test
    fun `명세의 3종을 모두 매핑한다`() {
        // 기간 만료 뒤 자동 삭제는 배치가 하고 클라는 404 로만 인지하므로 DELETED 같은 상태는 계약에 없다.
        val expected =
            mapOf(
                "UPCOMING" to ChallengeStatus.UPCOMING,
                "ACTIVE" to ChallengeStatus.ACTIVE,
                "COMPLETED" to ChallengeStatus.COMPLETED,
            )

        expected.forEach { (value, status) -> assertEquals(status, ChallengeStatus.fromValue(value)) }
        assertEquals(expected.size, ChallengeStatus.entries.size)
    }

    @Test
    fun `모르는 생애주기는 null 이다`() {
        assertNull(ChallengeStatus.fromValue("DELETED"))
        assertNull(ChallengeStatus.fromValue(null))
    }
}

class ChallengeFieldTest {
    @Test
    fun `수정 폼의 13개 필드가 서버 필드명과 하나씩 대응한다`() {
        // 서버는 editableFields 로 "무엇을 고칠 수 있는지"를 필드명으로 준다. 철자가 하나만 어긋나도
        // 그 항목은 조용히 잠긴 채로 남아, 방장은 고칠 수 있는 값을 고치지 못한다.
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
    fun `모르는 필드명은 수정 가능 목록에 끼어들지 못한다`() {
        // data 층이 mapNotNull 로 걸러 버리므로, 서버가 필드를 늘려도 폼은 아는 항목만 열어 둔다.
        assertNull(ChallengeField.fromValue("weekly_count"))
        assertNull(ChallengeField.fromValue(null))
    }
}
