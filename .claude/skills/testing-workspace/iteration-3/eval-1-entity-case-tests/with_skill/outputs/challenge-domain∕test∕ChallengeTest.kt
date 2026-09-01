package com.ruleup.challenge.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * `Challenge.kt` 의 enum 들은 **서버 문자열과 앱 화면 사이의 유일한 통역**이다. 값이 하나 어긋나면
 * 예외 없이 조용히 기본값(`?: SOLO`, `mapNotNull`)으로 접혀서, 잘못된 화면이 그려질 뿐 아무도 모른다.
 * 여기서 고정하는 건 "몇 종이 있고 그 서버 값이 무엇인가"이며, 문자열이 실제 응답으로 들어오는 경로는
 * data 의 매퍼 테스트가 본다.
 *
 * [CreateChallengeCommand] 의 불변식은 `CreateChallengeCommandTest` 가 이미 본다 — 여기서 다시 세지 않는다.
 */
class ChallengeModeTest {
    @Test
    fun `참여 형태는 SOLO·GROUP 두 종이고 서버 값과 이름이 같다`() {
        assertEquals(listOf("SOLO", "GROUP"), ChallengeMode.entries.map { it.value })
        ChallengeMode.entries.forEach { assertEquals(it, ChallengeMode.fromValue(it.value)) }
    }

    @Test
    fun `남과 함께하는 참여 형태는 GROUP 하나뿐이다`() {
        // 방 홈·랭킹·멤버 관리가 이 판정 하나로 열린다. SOLO 가 통과하면 존재하지 않는 방을 연다.
        assertEquals(listOf(ChallengeMode.GROUP), ChallengeMode.entries.filter { it.isGroup })
    }

    @Test
    fun `모르는 참여 형태는 null 이다`() {
        // 구 participationType 값이 남아 있어도 앱은 터지지 않고 data 가 정한 기본값으로 접힌다.
        assertNull(ChallengeMode.fromValue("PARTICIPATION_SOLO"))
        assertNull(ChallengeMode.fromValue(null))
    }
}

class ChallengeVisibilityTest {
    @Test
    fun `공개 범위는 PUBLIC·PRIVATE 두 종이고 서버 값과 이름이 같다`() {
        assertEquals(listOf("PUBLIC", "PRIVATE"), ChallengeVisibility.entries.map { it.value })
        ChallengeVisibility.entries.forEach { assertEquals(it, ChallengeVisibility.fromValue(it.value)) }
    }

    @Test
    fun `초대로만 들어올 수 있는 공개 범위는 PRIVATE 하나뿐이다`() {
        // 탐색 노출과 참여 게이트가 여기서 갈린다 — PUBLIC 이 통과하면 비공개 방이 목록에 샌다.
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
        // 만료 후 자동 삭제는 배치가 하므로 DELETED 같은 상태는 계약에 없다. 클라는 404 로만 인지한다.
        assertEquals(listOf("UPCOMING", "ACTIVE", "COMPLETED"), ChallengeStatus.entries.map { it.value })
        ChallengeStatus.entries.forEach { assertEquals(it, ChallengeStatus.fromValue(it.value)) }
    }

    @Test
    fun `모르는 생애주기 값은 null 이다`() {
        assertNull(ChallengeStatus.fromValue("DELETED"))
        assertNull(ChallengeStatus.fromValue(null))
    }
}

class ChallengeFieldTest {
    @Test
    fun `수정 폼 필드 13종은 명세의 요청 키를 그대로 쓴다`() {
        // enum 이름(SNAKE)과 요청 키(camel)가 다른 유일한 enum 이다. 한 글자만 어긋나도 서버는 그 필드를
        // 조용히 버리고, 화면은 "수정했는데 안 바뀐다"로 나타난다.
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
    fun `수정 폼 필드는 요청 키가 서로 겹치지 않는다`() {
        // 겹치면 editableFields 파싱에서 한쪽이 다른 쪽으로 접혀 폼이 엉뚱한 칸을 잠근다.
        assertEquals(ChallengeField.entries.size, ChallengeField.entries.map { it.value }.toSet().size)
    }

    @Test
    fun `모르는 수정 필드 이름은 null 이다`() {
        // settings.editableFields 는 mapNotNull 로 담으므로, 서버가 필드를 늘려도 폼이 터지지 않고 잠긴 채 남는다.
        assertNull(ChallengeField.fromValue("watcherPenalty"))
        assertNull(ChallengeField.fromValue(null))
    }
}

class ModerationStateTest {
    @Test
    fun `심사 상태는 4종이고 EXEMPT 를 따로 두지 않는다`() {
        // 앱은 심사 주체가 아니라 "심사 대상 아님"과 "통과"를 구분할 이유가 없어 data 가 EXEMPT 를
        // APPROVED 로 합친다. 여기에 EXEMPT 가 생기면 그 결정이 뒤집힌 것이므로 매퍼도 같이 고쳐야 한다.
        assertEquals(
            listOf("APPROVED", "IN_REVIEW", "REJECTED", "NONE"),
            ModerationState.entries.map { it.name },
        )
    }
}
