package com.ruleup.report.data.dto

import com.ruleup.network.dto.ApiException
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportContext
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.domain.entity.ReportTarget
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportRequestMappingTest {
    // 앱의 NetworkModule 과 같은 설정이다 — 여기서만 기본값을 내보내면 실제 요청과 달라진다.
    private val json = Json { encodeDefaults = false }

    @Test
    fun `프로필에서 한 사용자 신고는 챌린지 필드를 아예 보내지 않는다`() {
        // 빈 문자열로 보내면 서버가 parseUuid 에서 400 INVALID_REPORT_TARGET 을 낸다.
        val body =
            json.encodeToString(
                ReportRequest.serializer(),
                ReportTarget.User("u-1", ReportReason.SPAM_AD, ReportContext.PROFILE).toRequest(),
            )

        assertTrue("targetChallengeId" !in body, "빈 챌린지 필드가 실렸다: $body")
    }

    @Test
    fun `방에서 한 사용자 신고는 발생 챌린지를 함께 보낸다`() {
        val request =
            ReportTarget
                .User("u-1", ReportReason.CHEATING_SUSPECT, ReportContext.ROOM, challengeId = "c-9")
                .toRequest()

        assertEquals("USER", request.targetType)
        assertEquals("u-1", request.targetUserId)
        assertEquals("c-9", request.targetChallengeId)
        assertEquals("ROOM", request.contextType)
        assertEquals("CHEATING_SUSPECT", request.reason)
    }

    @Test
    fun `챌린지 신고는 대상 사용자를 비우고 챌린지만 보낸다`() {
        val request =
            ReportTarget
                .Challenge("c-9", ReportReason.INAPPROPRIATE, ReportContext.CHALLENGE_DETAIL)
                .toRequest()

        assertEquals("CHALLENGE", request.targetType)
        assertNull(request.targetUserId)
        assertEquals("c-9", request.targetChallengeId)
    }
}

class ReportResponseMappingTest {
    @Test
    fun `접수 응답의 가림 효과를 도메인 값으로 옮긴다`() {
        val result = ReportCreateResponse("r-1", "USER_CONTENT_MASKED").toDomain()

        assertEquals("r-1", result.reportId)
        assertEquals(HiddenEffect.USER_CONTENT_MASKED, result.hiddenEffect)
    }

    @Test
    fun `모르는 가림 효과가 와도 접수는 성공으로 남는다`() {
        // 접수는 이미 끝난 상태다. 효과 문구를 몰라 실패로 뒤집으면 사용자가 다시 신고한다.
        val result = ReportCreateResponse("r-1", "SOMETHING_NEW").toDomain()

        assertEquals("r-1", result.reportId)
        assertNull(result.hiddenEffect)
    }

    @Test
    fun `접수 식별자가 없으면 응답 필드 누락으로 막는다`() {
        val failure = assertFailsWith<ApiException> { ReportCreateResponse(null, "USER_CONTENT_MASKED").toDomain() }

        assertEquals("RESPONSE_FIELD_MISSING", failure.code)
    }
}

class BlockListMappingTest {
    @Test
    fun `차단 목록의 두 갈래를 모두 옮긴다`() {
        val blocks =
            BlockListResponse(
                users = listOf(BlockedUserResponse("u-1", "차단한 사용자", "2026-08-17T10:00:00Z")),
                challenges = listOf(BlockedChallengeResponse("c-1", "가려진 챌린지", true, "2026-08-18T10:00:00Z")),
            ).toDomain()

        assertEquals("u-1", blocks.users.single().userId)
        assertEquals("차단한 사용자", blocks.users.single().maskedNickname)
        assertEquals("c-1", blocks.challenges.single().challengeId)
        assertTrue(blocks.challenges.single().participating)
    }

    @Test
    fun `목록이 통째로 없으면 빈 목록으로 본다`() {
        // 차단한 게 없을 때 서버가 키를 생략해도 화면은 빈 상태를 그려야 한다.
        val blocks = BlockListResponse(users = null, challenges = null).toDomain()

        assertTrue(blocks.isEmpty)
    }

    @Test
    fun `참여 여부가 비면 미참여로 접는다`() {
        // 참여 중이라고 잘못 말하면 화면이 "방에서 나가기"를 권하게 된다.
        val challenge = BlockedChallengeResponse("c-1", "가려진 챌린지", null, null).toDomain()

        assertEquals(false, challenge.participating)
    }

    @Test
    fun `차단 목록 항목에 식별자가 없으면 막는다`() {
        // 그 행만 빼면 사용자는 차단이 남아 있는데 목록에 없는 상대를 영영 풀 수 없다.
        assertFailsWith<ApiException> {
            BlockListResponse(users = listOf(BlockedUserResponse(null, "차단한 사용자", null)), challenges = null).toDomain()
        }
    }
}

class ReportFailureMappingTest {
    @Test
    fun `명세의 에러 코드를 화면 어휘로 옮긴다`() {
        val mapped =
            listOf(
                "REPORT_SUSPENDED" to ReportFailure.SUSPENDED,
                "CANNOT_REPORT_SELF" to ReportFailure.SELF_TARGET,
                "INVALID_REPORT_TARGET" to ReportFailure.INVALID_TARGET,
                "INVALID_REPORT_REASON" to ReportFailure.INVALID_REASON,
                "USER_NOT_FOUND" to ReportFailure.TARGET_NOT_FOUND,
                "CHALLENGE_NOT_FOUND" to ReportFailure.TARGET_NOT_FOUND,
                "ACCOUNT_LOCKED" to ReportFailure.ACCOUNT_LOCKED,
                "BLOCK_ENTRY_NOT_FOUND" to ReportFailure.BLOCK_ENTRY_NOT_FOUND,
            )

        mapped.forEach { (code, expected) ->
            assertEquals(expected, ApiException(code, "").toReportFailure(), "$code 매핑이 어긋났다")
        }
    }

    @Test
    fun `모르는 에러 코드는 알 수 없음으로 떨어뜨린다`() {
        // 서버가 코드를 추가해도 앱이 멈추지 않아야 한다.
        assertEquals(ReportFailure.UNKNOWN, ApiException("REPORT_QUOTA_EXCEEDED", "").toReportFailure())
    }
}
