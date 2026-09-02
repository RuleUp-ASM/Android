package com.ruleup.report.data.fake

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.network.dto.ErrorBody
import com.ruleup.report.data.api.ReportApi
import com.ruleup.report.data.dto.BlockListResponse
import com.ruleup.report.data.dto.ReportCreateResponse
import com.ruleup.report.data.dto.ReportRequest

/**
 * 검증 대상만 값을 돌려준다. 나머지는 [NotImplementedError] 라, 의도치 않은 호출이 조용히 지나가지 않는다.
 */
class FakeReportApi(
    private val createResponse: BaseResponse<ReportCreateResponse>? = null,
    private val blockListResponse: BaseResponse<BlockListResponse>? = null,
    private val deleteResponse: BaseResponse<EmptyData>? = null,
    private val throwOnCall: Throwable? = null,
) : ReportApi {
    var lastRequest: ReportRequest? = null
        private set
    var unblockedUserId: String? = null
        private set
    var unblockedChallengeId: String? = null
        private set

    override suspend fun report(request: ReportRequest): BaseResponse<ReportCreateResponse> {
        lastRequest = request
        throwOnCall?.let { throw it }
        return createResponse ?: throw NotImplementedError()
    }

    override suspend fun getBlocks(): BaseResponse<BlockListResponse> {
        throwOnCall?.let { throw it }
        return blockListResponse ?: throw NotImplementedError()
    }

    override suspend fun unblockUser(blockedUserId: String): BaseResponse<EmptyData> {
        unblockedUserId = blockedUserId
        throwOnCall?.let { throw it }
        return deleteResponse ?: throw NotImplementedError()
    }

    override suspend fun unblockChallenge(challengeId: String): BaseResponse<EmptyData> {
        unblockedChallengeId = challengeId
        throwOnCall?.let { throw it }
        return deleteResponse ?: throw NotImplementedError()
    }
}

fun <T> ok(data: T): BaseResponse<T> = BaseResponse(success = true, data = data)

fun <T> failure(code: String): BaseResponse<T> =
    BaseResponse(success = false, data = null, error = ErrorBody(code = code, message = "테스트 실패 응답"))
