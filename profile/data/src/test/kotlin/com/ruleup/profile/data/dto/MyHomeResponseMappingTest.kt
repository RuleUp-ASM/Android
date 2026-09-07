package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * 마이 홈 응답 매핑. 이 화면은 **사용자가 자기 상태를 확인하는 곳**이라, 조용히 접힌 값이
 * 곧 "내 기록이 사라졌다"로 읽힌다.
 */
class MyHomeResponseMappingTest {
    @Test
    fun `집계를 통째로 안 주면 0 으로 채운다`() {
        // 카드 자체를 못 그리는 것보다는 0 이 낫다 — 사용자가 화면을 열 수는 있어야 한다.
        val home = MyHomeResponse(nickname = "지현", counts = null).toDomain()

        assertEquals(0, home.counts.inProgress)
        assertEquals(0, home.counts.completed)
        assertEquals(0, home.counts.left)
    }

    @Test
    fun `닉네임이 없으면 조용히 넘기지 않고 실패로 알린다`() {
        // 빈 닉네임으로 홈을 그리면 사용자가 자기 계정이 맞는지 알 수 없다.
        assertFailsWith<ApiException> { MyHomeResponse(nickname = null).toDomain() }
    }

    @Test
    fun `모르는 닉네임 검수 상태는 통과로 본다`() {
        // 서버가 상태를 넓혔을 때 구버전 앱이 멀쩡한 닉네임을 "검수 중"으로 묶어 두면,
        // 사용자는 하지도 않은 위반으로 기능이 막힌 것처럼 느낀다.
        val home = MyHomeResponse(nickname = "지현", nicknameStatus = "UNDER_REVIEW_V2").toDomain()

        assertEquals(NicknameStatus.APPROVED, home.nicknameStatus)
    }

    @Test
    fun `표시 티어를 안 주면 실제 티어로 떨어뜨린다`() {
        // 없는 유예를 있는 것처럼 그리면 못 들어가는 방을 들어갈 수 있는 것처럼 보여 준다.
        val home = MyHomeResponse(nickname = "지현", tier = "SILVER", displayTier = null).toDomain()

        assertEquals(Tier.SILVER, home.displayTier)
    }

    @Test
    fun `잠금 해제 시각이 없으면 잠금 안내를 만들지 않는다`() {
        // 사유·해제일 없는 잠금 배너는 불안만 주고 사용자가 할 수 있는 일이 없다.
        val home =
            MyHomeResponse(
                nickname = "지현",
                accountStatus = "LOCKED",
                lockInfo = LockInfoResponse(reason = "신고 검토", unlockAt = null),
            ).toDomain()

        assertEquals(AccountStatus.LOCKED, home.accountStatus)
        assertNull(home.lockInfo)
    }

    @Test
    fun `받은 티어와 집계는 그대로 전한다`() {
        val home =
            MyHomeResponse(
                nickname = "지현",
                tier = "GOLD",
                score = 370,
                displayTier = "GOLD",
                counts = MyHomeCountsResponse(inProgress = 1, completed = 3, left = 2),
            ).toDomain()

        assertEquals(Tier.GOLD, home.tier)
        assertEquals(370, home.score)
        assertEquals(3, home.counts.completed)
    }
}
