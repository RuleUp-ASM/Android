package com.ruleup.challenge.domain.entity

import com.ruleup.challenge.domain.fake.command
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 생성 요청이 스스로를 검증한다. 화면도 같은 범위로 입력을 막지만 그건 UX 이고, 여기 걸리는 건
 * 화면을 거치지 않는 경로(상태 복원·테스트·나중 리팩터링)에서 규칙이 빠졌다는 뜻이다.
 */
class CreateChallengeCommandTest {
    @Test
    fun `주간 횟수가 1~7 을 벗어나면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { command().copy(weeklyCount = 0) }
        assertFailsWith<IllegalArgumentException> { command().copy(weeklyCount = 8) }
    }

    @Test
    fun `그룹은 공개 범위와 정원이 있어야 하고 랭킹 공개 여부는 없어야 한다`() {
        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = 50,
            )

        assertFailsWith<IllegalArgumentException> { group.copy(visibility = null) }
        assertFailsWith<IllegalArgumentException> { group.copy(capacity = null) }
        assertFailsWith<IllegalArgumentException> { group.copy(rankingVisible = true) }
    }

    @Test
    fun `그룹 정원이 1~10,000 을 벗어나면 만들 수 없다`() {
        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = 50,
            )

        assertFailsWith<IllegalArgumentException> { group.copy(capacity = 0) }
        assertFailsWith<IllegalArgumentException> { group.copy(capacity = 10_001) }
    }

    @Test
    fun `경계값은 그대로 통과한다`() {
        // 거부만 확인하면 범위를 한 칸 좁혀도 테스트가 통과한다 — 양끝이 살아 있는지도 못 박는다.
        assertEquals(ChallengeLimits.WEEKLY_COUNT_MIN, command().copy(weeklyCount = 1).weeklyCount)
        assertEquals(ChallengeLimits.WEEKLY_COUNT_MAX, command().copy(weeklyCount = 7).weeklyCount)

        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = ChallengeLimits.CAPACITY_MIN,
            )

        assertEquals(1, group.capacity)
        assertEquals(10_000, group.copy(capacity = ChallengeLimits.CAPACITY_MAX).capacity)
    }

    @Test
    fun `솔로는 그룹 전용 필드를 채우면 안 되고 랭킹 공개 여부는 있어야 한다`() {
        // 솔로 기본형(fake) 이 이미 유효하므로, 그룹 필드를 채우는 쪽만 확인한다.
        assertFailsWith<IllegalArgumentException> { command().copy(visibility = ChallengeVisibility.PUBLIC) }
        assertFailsWith<IllegalArgumentException> { command().copy(capacity = 50) }
        assertFailsWith<IllegalArgumentException> { command().copy(rankingVisible = null) }
    }
}
