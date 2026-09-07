package com.ruleup.report.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.getOrThrow
import com.ruleup.network.dto.throwOnError
import com.ruleup.report.data.api.ReportApi
import com.ruleup.report.data.dto.toDomain
import com.ruleup.report.data.dto.toReportFailure
import com.ruleup.report.data.dto.toRequest
import com.ruleup.report.domain.entity.BlockList
import com.ruleup.report.domain.entity.ReportException
import com.ruleup.report.domain.entity.ReportFailure
import com.ruleup.report.domain.entity.ReportResult
import com.ruleup.report.domain.entity.ReportTarget
import com.ruleup.report.domain.repository.ReportRepository
import java.io.IOException
import javax.inject.Inject

class ReportRepositoryImpl
    @Inject
    constructor(
        private val api: ReportApi,
    ) : ReportRepository {
        override suspend fun report(target: ReportTarget): ReportResult =
            translating {
                api.report(target.toRequest()).getOrThrow().toDomain()
            }

        override suspend fun getBlocks(): BlockList =
            translating {
                api.getBlocks().getOrThrow().toDomain()
            }

        override suspend fun unblockUser(userId: String) =
            translating {
                api.unblockUser(userId).throwOnError()
            }

        override suspend fun unblockChallenge(challengeId: String) =
            translating {
                api.unblockChallenge(challengeId).throwOnError()
            }

        /**
         * 모든 실패를 [ReportException] 하나로 모은다. 화면이 `ApiException` 코드 문자열을 읽지
         * 않게 하려는 것이고, [IOException] 을 따로 잡는 이유는 "다시 시도"를 권할 수 있는
         * 실패인지가 거기서 갈리기 때문이다 — 서버가 거절한 것과 아예 닿지 못한 것은 다르다.
         */
        private inline fun <T> translating(block: () -> T): T =
            try {
                block()
            } catch (e: ApiException) {
                throw ReportException(e.toReportFailure(), e.message.orEmpty(), e)
            } catch (e: IOException) {
                throw ReportException(ReportFailure.NETWORK, "지금은 연결이 불안정해요. 잠시 후 다시 시도해 주세요.", e)
            }
    }
