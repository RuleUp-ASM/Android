package com.ruleup.verification.data.repository

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.ErrorBody
import com.ruleup.verification.data.api.KakaoLocalApi
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.ChallengeSetupRequest
import com.ruleup.verification.data.dto.ChallengeSetupResponse
import com.ruleup.verification.data.dto.IntroRequest
import com.ruleup.verification.data.dto.IntroResponse
import com.ruleup.verification.data.dto.KakaoCoord2AddressResponse
import com.ruleup.verification.data.dto.KakaoKeywordResponse
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ManualSubmitResponse
import com.ruleup.verification.data.dto.ProgressResponse
import com.ruleup.verification.data.dto.SyncEnvelopeRequest
import com.ruleup.verification.data.dto.SyncResponse
import com.ruleup.verification.data.dto.VerificationDetailResponse
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.FallbackLimitExceededException
import com.ruleup.verification.domain.entity.ManualMethod
import com.ruleup.verification.domain.entity.VerifiedVia
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerificationRepositoryImplTest {
    @Test
    fun `수동 제출 409 ALREADY_VERIFIED 는 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(manualError = ErrorBody("ALREADY_VERIFIED", "이미 인증함"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<AlreadyVerifiedException> {
                repository.submitManual("c1", ManualMethod.SELF_CHECK)
            }
        }

    @Test
    fun `수동 제출 성공은 결과를 매핑한다`() =
        runTest {
            val api =
                FakeVerificationApi(
                    manualSuccess =
                        ManualSubmitResponse(
                            targetDate = "2026-06-21",
                            status = "SUCCESS",
                            method = "SELF_CHECK",
                            progressRate = 60.0,
                        ),
                )
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            val result = repository.submitManual("c1", ManualMethod.SELF_CHECK)

            assertEquals("2026-06-21", result.targetDate)
            assertEquals(ManualMethod.SELF_CHECK, result.method)
            assertEquals(60.0, result.progressRate)
            // verifiedVia 누락 시 보수적으로 MANUAL(명세 §9.2).
            assertEquals(VerifiedVia.MANUAL, result.verifiedVia)
        }

    @Test
    fun `예비 폴백 409 FALLBACK_LIMIT_EXCEEDED 는 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(manualError = ErrorBody("FALLBACK_LIMIT_EXCEEDED", "한도 초과"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<FallbackLimitExceededException> {
                repository.submitManual("c1", ManualMethod.SELF_CHECK, asFallback = true)
            }
        }

    @Test
    fun `예비 폴백 성공은 verifiedVia·disputeClosesAt 을 매핑한다`() =
        runTest {
            val api =
                FakeVerificationApi(
                    manualSuccess =
                        ManualSubmitResponse(
                            targetDate = "2026-06-24",
                            status = "SUCCESS",
                            method = "SELF_CHECK",
                            progressRate = 50.0,
                            verifiedVia = "MANUAL_FALLBACK",
                            disputeClosesAt = "2026-06-25T00:00:00Z",
                        ),
                )
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            val result = repository.submitManual("c1", ManualMethod.SELF_CHECK, asFallback = true)

            assertEquals(VerifiedVia.MANUAL_FALLBACK, result.verifiedVia)
            assertEquals("2026-06-25T00:00:00Z", result.disputeClosesAt)
        }

    private class FakeVerificationApi(
        private val manualSuccess: ManualSubmitResponse? = null,
        private val manualError: ErrorBody? = null,
    ) : VerificationApi {
        override suspend fun intro(request: IntroRequest): BaseResponse<IntroResponse> = error("unused")

        override suspend fun sync(request: SyncEnvelopeRequest): BaseResponse<SyncResponse> = error("unused")

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

        override suspend fun setup(
            challengeId: String,
            request: ChallengeSetupRequest,
        ): BaseResponse<ChallengeSetupResponse> = error("unused")
    }

    private class FakeKakaoLocalApi : KakaoLocalApi {
        override suspend fun searchKeyword(
            query: String,
            longitude: Double?,
            latitude: Double?,
            radiusM: Int?,
            size: Int,
            sort: String,
        ): KakaoKeywordResponse = KakaoKeywordResponse(documents = emptyList())

        override suspend fun coord2Address(
            longitude: Double,
            latitude: Double,
        ): KakaoCoord2AddressResponse = KakaoCoord2AddressResponse(documents = emptyList())
    }
}
