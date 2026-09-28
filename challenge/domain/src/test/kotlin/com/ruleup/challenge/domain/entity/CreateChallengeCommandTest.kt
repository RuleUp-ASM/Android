package com.ruleup.challenge.domain.entity

import com.ruleup.challenge.domain.fake.command
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** 생성 요청이 스스로를 검증한다. */
class CreateChallengeCommandTest {
    @Test
    fun `주간 횟수가 1~7 을 벗어나면 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> { command().copy(weeklyCount = 0) }
        assertFailsWith<IllegalArgumentException> { command().copy(weeklyCount = 8) }
    }

    @Test
    fun `그룹은 공개 범위가 있어야 하고 랭킹 공개 여부는 없어야 한다`() {
        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = 30,
            )

        assertFailsWith<IllegalArgumentException> { group.copy(visibility = null) }
        assertFailsWith<IllegalArgumentException> { group.copy(rankingVisible = true) }
    }

    @Test
    fun `그룹 정원이 단계 5·30·100·300·무제한(null) 밖이면 만들 수 없다`() {
        val group =
            command().copy(
                mode = ChallengeMode.GROUP,
                visibility = ChallengeVisibility.PUBLIC,
                rankingVisible = null,
                capacity = 30,
            )

        assertFailsWith<IllegalArgumentException> { group.copy(capacity = 0) }
        // 단계 사이 값
        assertFailsWith<IllegalArgumentException> { group.copy(capacity = 50) }
        assertFailsWith<IllegalArgumentException> { group.copy(capacity = 10_001) }
        // 무제한(null)을 포함한 모든 단계는 만들어진다.
        ChallengeLimits.CREATE_CAPACITY_STEPS.forEach { group.copy(capacity = it) }
    }

    @Test
    fun `솔로는 그룹 전용 필드를 채우면 안 되고 랭킹 공개 여부는 있어야 한다`() {
        // 솔로 기본형(fake) 이 이미 유효하므로, 그룹 필드를 채우는 쪽만 확인한다.
        assertFailsWith<IllegalArgumentException> { command().copy(visibility = ChallengeVisibility.PUBLIC) }
        assertFailsWith<IllegalArgumentException> { command().copy(capacity = 30) }
        assertFailsWith<IllegalArgumentException> { command().copy(rankingVisible = null) }
    }
}

/** 초안 정원을 단계로 맞추는 규칙. */
class CreateCapacityStepTest {
    @Test
    fun `초안 정원은 그 이상인 가장 가까운 단계로 올리고 300 을 넘거나 무제한이면 무제한이 된다`() {
        assertEquals(5, ChallengeLimits.createCapacityStepAtLeast(1))
        assertEquals(30, ChallengeLimits.createCapacityStepAtLeast(30))
        assertEquals(100, ChallengeLimits.createCapacityStepAtLeast(50))
        assertEquals(300, ChallengeLimits.createCapacityStepAtLeast(300))
        assertNull(ChallengeLimits.createCapacityStepAtLeast(301))
        assertNull(ChallengeLimits.createCapacityStepAtLeast(null))
    }
}
