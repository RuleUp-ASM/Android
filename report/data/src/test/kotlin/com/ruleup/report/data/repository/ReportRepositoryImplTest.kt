package com.ruleup.report.data.repository

import com.ruleup.network.dto.EmptyData
import com.ruleup.report.data.dto.BlockListResponse
import com.ruleup.report.data.dto.BlockedUserResponse
import com.ruleup.report.data.dto.ReportCreateResponse
import com.ruleup.report.data.fake.FakeReportApi
import com.ruleup.report.data.fake.failure
import com.ruleup.report.data.fake.ok
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportContext
import com.ruleup.report.domain.entity.ReportException
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.domain.entity.ReportTarget
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReportRepositoryImplTest {
    private val profileReport =
        ReportTarget.User("u-1", ReportReason.SPAM_AD, ReportContext.PROFILE)

    @Test
    fun `신고 접수는 대상을 요청으로 옮겨 보내고 결과를 돌려준다`() =
        runTest {
            val api = FakeReportApi(createResponse = ok(ReportCreateResponse("r-1", "USER_CONTENT_MASKED")))

            val result = ReportRepositoryImpl(api).report(profileReport)

            assertEquals("USER", api.lastRequest?.targetType)
            assertEquals("u-1", api.lastRequest?.targetUserId)
            assertEquals("r-1", result.reportId)
            assertEquals(HiddenEffect.USER_CONTENT_MASKED, result.hiddenEffect)
        }

    @Test
    fun `신고 기능이 정지되면 정지 실패로 번역한다`() =
        runTest {
            // 화면이 "다시 시도"를 권하면 안 되는 유일한 실패다 — 재시도로 풀리지 않는다.
            val api = FakeReportApi(createResponse = failure("REPORT_SUSPENDED"))

            val thrown = assertFailsWith<ReportException> { ReportRepositoryImpl(api).report(profileReport) }

            assertEquals(ReportFailure.SUSPENDED, thrown.failure)
        }

    @Test
    fun `본인 신고 거절을 자기 자신 실패로 번역한다`() =
        runTest {
            val api = FakeReportApi(createResponse = failure("CANNOT_REPORT_SELF"))

            val thrown = assertFailsWith<ReportException> { ReportRepositoryImpl(api).report(profileReport) }

            assertEquals(ReportFailure.SELF_TARGET, thrown.failure)
        }

    @Test
    fun `서버에 닿지 못하면 네트워크 실패로 번역한다`() =
        runTest {
            // 서버가 거절한 것과 아예 닿지 못한 것을 화면이 구분해야 "다시 시도"를 권할 수 있다.
            val api = FakeReportApi(throwOnCall = IOException("offline"))

            val thrown = assertFailsWith<ReportException> { ReportRepositoryImpl(api).report(profileReport) }

            assertEquals(ReportFailure.NETWORK, thrown.failure)
        }

    @Test
    fun `차단 목록을 도메인 목록으로 돌려준다`() =
        runTest {
            val api =
                FakeReportApi(
                    blockListResponse =
                        ok(
                            BlockListResponse(
                                users = listOf(BlockedUserResponse("u-1", "차단한 사용자", null)),
                                challenges = null,
                            ),
                        ),
                )

            val blocks = ReportRepositoryImpl(api).getBlocks()

            assertEquals("u-1", blocks.users.single().userId)
            assertEquals(emptyList(), blocks.challenges)
        }

    @Test
    fun `사용자 차단 해제는 대상 id 를 그대로 넘긴다`() =
        runTest {
            val api = FakeReportApi(deleteResponse = ok(EmptyData()))

            ReportRepositoryImpl(api).unblockUser("u-1")

            assertEquals("u-1", api.unblockedUserId)
        }

    @Test
    fun `챌린지 차단 해제는 대상 id 를 그대로 넘긴다`() =
        runTest {
            val api = FakeReportApi(deleteResponse = ok(EmptyData()))

            ReportRepositoryImpl(api).unblockChallenge("c-1")

            assertEquals("c-1", api.unblockedChallengeId)
        }

    @Test
    fun `이미 풀린 차단을 다시 풀면 차단 내역 없음으로 번역한다`() =
        runTest {
            // 목록을 열어 둔 채 다른 기기에서 해제한 경우다. 화면은 목록만 새로 고치면 된다.
            val api = FakeReportApi(deleteResponse = failure("BLOCK_ENTRY_NOT_FOUND"))

            val thrown = assertFailsWith<ReportException> { ReportRepositoryImpl(api).unblockUser("u-1") }

            assertEquals(ReportFailure.BLOCK_ENTRY_NOT_FOUND, thrown.failure)
        }
}
