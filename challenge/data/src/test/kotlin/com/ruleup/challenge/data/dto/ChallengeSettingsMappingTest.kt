package com.ruleup.challenge.data.dto

import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.network.dto.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 챌린지 설정 응답 매핑. 이 화면은 받은 값을 그대로 **수정 폼의 초기값**으로 쓰므로, 잘못 접힌
 * 기본값이 곧 사용자가 저장하게 될 값이 된다 — 조회 때 조용히 틀린 값은 저장 때 진짜가 된다.
 */
class ChallengeSettingsMappingTest {
    @Test
    fun `주간 횟수를 안 주면 매일로 본다`() {
        // 0 으로 접으면 "아무 날도 안 함"이 돼 화면이 거짓말을 한다.
        val settings = response(weeklyCount = null).toDomain()

        assertEquals(7, settings.config.weeklyCount)
    }

    @Test
    fun `주간 횟수가 범위를 벗어나면 범위 안으로 접는다`() {
        // domain 의 CreateChallengeCommand 는 1~7 을 불변식으로 갖는다 — 벗어난 값을 그대로
        // 넘기면 화면이 아니라 저장 시점에 터진다.
        assertEquals(7, response(weeklyCount = 9).toDomain().config.weeklyCount)
        assertEquals(1, response(weeklyCount = 0).toDomain().config.weeklyCount)
    }

    @Test
    fun `모르는 참여 형태는 솔로로 접는다`() {
        // 상세 매퍼는 같은 상황에서 GROUP 으로 접는다 — 같은 미지 값이 화면마다 다르게 보인다.
        // 어느 쪽이 맞는지는 정책 판단이라 여기서는 현재 동작만 못 박는다.
        val settings = response(mode = "COUPLE").toDomain()

        assertEquals(ChallengeMode.SOLO, settings.config.mode)
    }

    @Test
    fun `설정 본문이 없으면 조용히 넘기지 않고 실패로 알린다`() {
        // 빈 폼을 열어 주면 사용자가 그 상태로 저장해 실제 설정을 지운다.
        assertFailsWith<ApiException> { ChallengeSettingsResponse(config = null).toDomain() }
    }

    @Test
    fun `앱이 모르는 수정 가능 필드는 버린다`() {
        // 서버가 필드를 추가해도 구버전 앱이 폼을 잘못 여는 것보다 낫다.
        val settings = response(editableFields = listOf("TITLE", "SOMETHING_NEW")).toDomain()

        assertTrue(settings.editableFields.none { it.value == "SOMETHING_NEW" })
    }

    private fun response(
        weeklyCount: Int? = 5,
        mode: String? = "GROUP",
        editableFields: List<String>? = listOf("TITLE"),
    ) = ChallengeSettingsResponse(
        config =
            ChallengeConfigResponse(
                title = "아침 6시 기상",
                description = "매일 아침",
                category = "EXERCISE",
                mode = mode,
                weeklyCount = weeklyCount,
                period = PeriodDto(start = "2026-09-01", end = "2026-10-01"),
                verification = VerificationDto(type = "MANUAL", method = "SELF_CHECK"),
                penalties = PenaltiesDto(score = true, groupShare = true, watcher = false),
            ),
        editableFields = editableFields,
        version = 1,
    )
}
