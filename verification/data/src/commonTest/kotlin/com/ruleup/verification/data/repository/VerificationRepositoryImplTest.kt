package com.ruleup.verification.data.repository

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.ErrorBody
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ManualSubmitResponse
import com.ruleup.verification.data.dto.ProgressResponse
import com.ruleup.verification.data.dto.SyncRequest
import com.ruleup.verification.data.dto.SyncResponse
import com.ruleup.verification.data.dto.VerificationDetailResponse
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.ManualMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerificationRepositoryImplTest {
    @Test
    fun `수동 제출 409 ALREADY_VERIFIED 는 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(manualError = ErrorBody("ALREADY_VERIFIED", "이미 인증함"))
            val repository = VerificationRepositoryImpl(api)

            assertFailsWith<AlreadyVerifiedException> {
                repository.submitManual("c1", ManualMethod.SELF_CHECK)
            }
        }

    @Test
    fun `수동 제출 성공은 결과를 매핑한다`() =
        runTest {
            val api =
                FakeVerificationApi(
                    manualSuccess = ManualSubmitResponse(targetDate = "2026-06-21", status = "SUCCESS", method = "SELF_CHECK", progressRate = 60.0),
                )
            val repository = VerificationRepositoryImpl(api)

            val result = repository.submitManual("c1", ManualMethod.SELF_CHECK)

            assertEquals("2026-06-21", result.targetDate)
            assertEquals(ManualMethod.SELF_CHECK, result.method)
            assertEquals(60.0, result.progressRate)
        }

    private class FakeVerificationApi(
        private val manualSuccess: ManualSubmitResponse? = null,
        private val manualError: ErrorBody? = null,
    ) : VerificationApi {
        override suspend fun sync(request: SyncRequest): BaseResponse<SyncResponse> = error("unused")

        override suspend fun getProgress(status: String?): BaseResponse<ProgressResponse> = error("unused")

        override suspend fun getVerification(
            challengeId: String,
            logDays: Int?,
        ): BaseResponse<VerificationDetailResponse> = error("unused")

        override suspend fun submitManual(
            challengeId: String,
            request: ManualSubmitRequest,
        ): BaseResponse<ManualSubmitResponse> =
            if (manualError != null) {
                BaseResponse(success = false, data = null, error = manualError)
            } else {
                BaseResponse(success = true, data = manualSuccess, error = null)
            }
    }
}
