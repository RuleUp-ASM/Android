package com.ruleup.report.domain.repository

import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.report.domain.entity.ReportTarget

/** 신고 접수와 개인 차단. */
interface ReportRepository {
    /** 신고를 접수한다. */
    suspend fun report(target: ReportTarget): ReportResult

    /** 내가 차단한 사용자·챌린지 목록. */
    suspend fun getBlocks(): BlockList

    /** 사용자 차단을 푼다. */
    suspend fun unblockUser(userId: String)

    /** 챌린지 차단을 푼다. */
    suspend fun unblockChallenge(challengeId: String)
}
