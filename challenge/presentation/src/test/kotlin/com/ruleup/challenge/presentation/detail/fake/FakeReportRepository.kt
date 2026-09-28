package com.ruleup.challenge.presentation.detail.fake

import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.report.domain.entity.ReportTarget
import com.ruleup.report.domain.repository.ReportRepository

/** 테스트 대상 API 대역. */
class FakeReportRepository(
    private val result: ReportResult = ReportResult("r-1", HiddenEffect.CHALLENGE_HIDDEN),
    private val error: Throwable? = null,
) : ReportRepository {
    val reported = mutableListOf<ReportTarget>()

    override suspend fun report(target: ReportTarget): ReportResult {
        reported += target
        error?.let { throw it }
        return result
    }

    override suspend fun getBlocks(): BlockList = throw NotImplementedError()

    override suspend fun unblockUser(userId: String) = throw NotImplementedError()

    override suspend fun unblockChallenge(challengeId: String) = throw NotImplementedError()
}
