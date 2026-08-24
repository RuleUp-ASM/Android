package com.ruleup.verification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.ErrorBody
import com.ruleup.verification.data.api.KakaoLocalApi
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.AcknowledgeResponse
import com.ruleup.verification.data.dto.AnchorDto
import com.ruleup.verification.data.dto.CancelManualResponse
import com.ruleup.verification.data.dto.ChallengeSetupRequest
import com.ruleup.verification.data.dto.ChallengeSetupResponse
import com.ruleup.verification.data.dto.IntroRequest
import com.ruleup.verification.data.dto.IntroResponse
import com.ruleup.verification.data.dto.KakaoCoord2AddressResponse
import com.ruleup.verification.data.dto.KakaoKeywordResponse
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ManualSubmitResponse
import com.ruleup.verification.data.dto.MyLocationResponse
import com.ruleup.verification.data.dto.MyScreenAppsResponse
import com.ruleup.verification.data.dto.ProgressResponse
import com.ruleup.verification.data.dto.StreakResponse
import com.ruleup.verification.data.dto.SyncEnvelopeRequest
import com.ruleup.verification.data.dto.SyncResponse
import com.ruleup.verification.data.dto.UpdateMyLocationRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsResponse
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.CancelWindowClosedException
import com.ruleup.verification.domain.entity.InvalidAnchorException
import com.ruleup.verification.domain.entity.InvalidTargetDateException
import com.ruleup.verification.domain.entity.LocationLockedInWindowException
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.SettingChangeLimitException
import com.ruleup.verification.domain.entity.TodayResultStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class VerificationRepositoryImplTest {
    @Test
    fun `수동 제출 409 ALREADY_VERIFIED 는 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(manualError = ErrorBody("ALREADY_VERIFIED", "이미 인증함"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<AlreadyVerifiedException> { repository.submitManual("c1") }
        }

    @Test
    fun `수동 제출 성공은 취소 키와 연속 기록을 매핑한다`() =
        runTest {
            val api =
                FakeVerificationApi(
                    manualSuccess =
                        ManualSubmitResponse(
                            verificationId = "v_11",
                            targetDate = "2026-07-25",
                            status = "DONE",
                            streak = StreakResponse(before = 6, after = 7),
                            scoreNote = "MANUAL_NO_SCORE",
                        ),
                )
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            val result = repository.submitManual("c1")

            // verificationId 가 없으면 방금 한 체크를 되돌릴 경로가 없다.
            assertEquals("v_11", result.verificationId)
            assertEquals("2026-07-25", result.targetDate)
            // 제출 즉시 확정 — 잠정 상태가 없다.
            assertEquals(TodayResultStatus.DONE, result.status)
            assertEquals(7, result.streak?.after)
            assertEquals("MANUAL_NO_SCORE", result.scoreNote)
        }

    @Test
    fun `자정을 넘긴 수동 제출은 도메인 예외로 변환된다`() =
        runTest {
            // 화면을 열어 둔 채 날짜가 바뀌면 실제로 난다 — 일반 오류 문구로 뭉개면 왜 막혔는지 알 수 없다.
            val api = FakeVerificationApi(manualError = ErrorBody("INVALID_TARGET_DATE", "오늘이 아님"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<InvalidTargetDateException> { repository.submitManual("c1") }
        }

    @Test
    fun `자동 방 수동 제출은 도메인 어휘로 올리지 않는다`() =
        runTest {
            // 화면이 자동 방에 체크 버튼을 두지 않는 것이 전제다 — 도달하면 프로그래밍 오류다.
            val api = FakeVerificationApi(manualError = ErrorBody("NOT_MANUAL_CHALLENGE", "자동 방"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<ApiException> { repository.submitManual("c1") }
        }

    @Test
    fun `앵커 교체는 적용 결과와 다음 변경 가능 시각을 매핑한다`() =
        runTest {
            val api =
                FakeVerificationApi(
                    myLocationSuccess =
                        MyLocationResponse(
                            anchors = listOf(AnchorDto(lat = 37.4979, lng = 127.0276, label = "새 헬스장")),
                            serverRadiusM = 500,
                            appliedFrom = "IMMEDIATE",
                            nextChangeAvailableAt = "2026-09-01T00:00:00+09:00",
                        ),
                )
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            val result = repository.updateMyLocation("c1", anchorSet())

            assertEquals(1, result.anchors.size)
            assertEquals(500f, result.serverRadiusM)
            assertEquals("2026-09-01T00:00:00+09:00", result.nextChangeAvailableAt)
            // 저장으로 그 달 1회를 소진했으므로 응답에 changeAvailable 이 없다 — 못 바꾸는 쪽으로 접는다.
            assertFalse(result.changeAvailable)
        }

    @Test
    fun `인증 윈도우 중 앵커 교체는 도메인 예외로 변환된다`() =
        runTest {
            // 화면이 "익일 재시도"를 안내해야 하는 실패다 — 일반 오류 문구로 뭉개면 사용자가 왜 막혔는지 모른다.
            val api = FakeVerificationApi(myLocationError = ErrorBody("LOCATION_LOCKED_IN_WINDOW", "진행 중"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<LocationLockedInWindowException> { repository.updateMyLocation("c1", anchorSet()) }
        }

    @Test
    fun `월 1회 소진은 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(myLocationError = ErrorBody("SETTING_CHANGE_LIMIT", "소진"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<SettingChangeLimitException> { repository.updateMyLocation("c1", anchorSet()) }
        }

    @Test
    fun `앵커 개수 초과도 입력 수정 안내로 흐른다`() =
        runTest {
            // 클라가 먼저 막지만 서버가 되돌려주면 같은 인라인 안내를 쓴다.
            val api = FakeVerificationApi(myLocationError = ErrorBody("ANCHOR_LIMIT_EXCEEDED", "4개"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<InvalidAnchorException> { repository.updateMyLocation("c1", anchorSet()) }
        }

    @Test
    fun `판정 결과 확인은 해당 인증 건으로 호출된다`() =
        runTest {
            val api = FakeVerificationApi()
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            repository.acknowledgeResult("v_9911")

            assertEquals("v_9911", api.acknowledgedId)
        }

    @Test
    fun `수동 인증 취소 기한 경과는 도메인 예외로 변환된다`() =
        runTest {
            // 체크를 되돌릴 수 없는 이유를 화면이 말해 줘야 한다.
            val api = FakeVerificationApi(cancelError = ErrorBody("CANCEL_WINDOW_CLOSED", "기한 경과"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<CancelWindowClosedException> { repository.cancelManual("v_1") }
        }

    @Test
    fun `자동 판정 건 취소는 도메인 어휘로 올리지 않는다`() =
        runTest {
            // 화면이 자동 방에 취소 버튼을 두지 않는 것이 전제다 — 도달하면 프로그래밍 오류지 사용자 안내가 아니다.
            val api = FakeVerificationApi(cancelError = ErrorBody("NOT_MANUAL_VERIFICATION", "자동 건"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi())

            assertFailsWith<ApiException> { repository.cancelManual("v_1") }
        }

    private fun anchorSet(): AnchorSet = AnchorSet.of(listOf(LocationPin(lat = 37.4979, lng = 127.0276, label = "새 헬스장")))

    private class FakeVerificationApi(
        private val manualSuccess: ManualSubmitResponse? = null,
        private val manualError: ErrorBody? = null,
        private val myLocationSuccess: MyLocationResponse? = null,
        private val myLocationError: ErrorBody? = null,
        private val cancelError: ErrorBody? = null,
    ) : VerificationApi {
        var acknowledgedId: String? = null

        override suspend fun acknowledgeResult(verificationId: String): BaseResponse<AcknowledgeResponse> {
            acknowledgedId = verificationId
            return BaseResponse(success = true, data = AcknowledgeResponse(acknowledged = true), error = null)
        }

        override suspend fun cancelManual(verificationId: String): BaseResponse<CancelManualResponse> =
            if (cancelError != null) {
                BaseResponse(success = false, data = null, error = cancelError)
            } else {
                BaseResponse(success = true, data = CancelManualResponse(canceled = true), error = null)
            }

        override suspend fun updateMyLocation(
            challengeId: String,
            request: UpdateMyLocationRequest,
        ): BaseResponse<MyLocationResponse> =
            if (myLocationError != null) {
                BaseResponse(success = false, data = null, error = myLocationError)
            } else {
                BaseResponse(success = true, data = myLocationSuccess, error = null)
            }

        override suspend fun intro(request: IntroRequest): BaseResponse<IntroResponse> = error("unused")

        override suspend fun sync(request: SyncEnvelopeRequest): BaseResponse<SyncResponse> = error("unused")

        override suspend fun getProgress(status: String?): BaseResponse<ProgressResponse> = error("unused")

        override suspend fun getTodayResult(challengeId: String) = error("unused")

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

        override suspend fun getMyLocation(challengeId: String): BaseResponse<MyLocationResponse> = error("unused")

        override suspend fun getMyScreenApps(challengeId: String): BaseResponse<MyScreenAppsResponse> = error("unused")

        override suspend fun updateMyScreenApps(
            challengeId: String,
            request: UpdateScreenAppsRequest,
        ): BaseResponse<UpdateScreenAppsResponse> = error("unused")

        override suspend fun submitAppeal(
            verificationId: String,
            request: com.ruleup.verification.data.dto.SubmitAppealRequest,
        ): BaseResponse<com.ruleup.verification.data.dto.AppealResponse> = error("unused")
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
