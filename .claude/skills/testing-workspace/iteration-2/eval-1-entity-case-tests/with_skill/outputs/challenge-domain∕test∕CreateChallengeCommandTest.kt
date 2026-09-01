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
    fun `주간 횟수 1회와 7회는 만들 수 있다`() {
        // 바깥쪽만 막아 두면 범위가 2~6 으로 좁아져도 아무 테스트도 깨지지 않는다 — 안쪽 끝을 함께 고정한다.
        assertEquals(ChallengeLimits.WEEKLY_COUNT_MIN, command().copy(weeklyCount = ChallengeLimits.WEEKLY_COUNT_MIN).weeklyCount)
        assertEquals(ChallengeLimits.WEEKLY_COUNT_MAX, command().copy(weeklyCount = ChallengeLimits.WEEKLY_COUNT_MAX).weeklyCount)
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
    fun `그룹 정원 1명과 10,000명은 만들 수 있다`() {
        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = 50,
            )

        assertEquals(ChallengeLimits.CAPACITY_MIN, group.copy(capacity = ChallengeLimits.CAPACITY_MIN).capacity)
        assertEquals(ChallengeLimits.CAPACITY_MAX, group.copy(capacity = ChallengeLimits.CAPACITY_MAX).capacity)
    }

    @Test
    fun `솔로는 그룹 전용 필드를 채우면 안 되고 랭킹 공개 여부는 있어야 한다`() {
        // 솔로 기본형(fake) 이 이미 유효하므로, 그룹 필드를 채우는 쪽만 확인한다.
        assertFailsWith<IllegalArgumentException> { command().copy(visibility = ChallengeVisibility.PUBLIC) }
        assertFailsWith<IllegalArgumentException> { command().copy(capacity = 50) }
        assertFailsWith<IllegalArgumentException> { command().copy(rankingVisible = null) }
    }
}
