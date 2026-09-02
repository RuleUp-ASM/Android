package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.network.dto.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 마이 홈 응답 매핑. 이 화면은 **사용자가 자기 상태를 확인하는 곳**이라, 조용히 접힌 값이
 * 곧 "내 기록이 사라졌다"로 읽힌다.
 */
class MyHomeResponseMappingTest {
    @Test
    fun `집계를 통째로 안 주면 0 으로 채운다`() {
        // 카드 자체를 못 그리는 것보다는 0 이 낫다 — 사용자가 화면을 열 수는 있어야 한다.
        val home = MyHomeResponse(nickname = "지현", counts = null).toDomain()

        assertEquals(0, home.counts.completed)
        assertEquals(0, home.counts.inProgress)
        assertEquals(0, home.counts.groups)
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
    fun `온도를 안 주면 0 도로 둔다`() {
        val home = MyHomeResponse(nickname = "지현", mannerTemperature = null).toDomain()

        assertEquals(0.0, home.mannerTemperature)
    }

    @Test
    fun `받은 집계는 그대로 전한다`() {
        val home =
            MyHomeResponse(
                nickname = "지현",
                mannerTemperature = 36.7,
                counts = MyHomeCountsResponse(completed = 3, inProgress = 1, groups = 2),
            ).toDomain()

        assertEquals(36.7, home.mannerTemperature)
        assertEquals(3, home.counts.completed)
    }
}
