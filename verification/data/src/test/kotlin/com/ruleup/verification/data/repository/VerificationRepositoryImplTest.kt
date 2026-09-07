package com.ruleup.verification.data.repository

import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.ErrorBody
import com.ruleup.network.image.ImageBytes
import com.ruleup.network.image.ImageReader
import com.ruleup.verification.data.api.KakaoLocalApi
import com.ruleup.verification.data.api.VerificationApi
import com.ruleup.verification.data.dto.AcknowledgeResponse
import com.ruleup.verification.data.dto.AnchorDto
import com.ruleup.verification.data.dto.AppealHistoryItemResponse
import com.ruleup.verification.data.dto.AppealImageResponse
import com.ruleup.verification.data.dto.AppealResponse
import com.ruleup.verification.data.dto.CancelManualResponse
import com.ruleup.verification.data.dto.ChallengeSetupRequest
import com.ruleup.verification.data.dto.ChallengeSetupResponse
import com.ruleup.verification.data.dto.IntroRequest
import com.ruleup.verification.data.dto.IntroResponse
import com.ruleup.verification.data.dto.KakaoCoord2AddressResponse
import com.ruleup.verification.data.dto.KakaoKeywordResponse
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ManualSubmitResponse
import com.ruleup.verification.data.dto.MyAppealsResponse
import com.ruleup.verification.data.dto.MyLocationResponse
import com.ruleup.verification.data.dto.MyScreenAppsResponse
import com.ruleup.verification.data.dto.ProgressResponse
import com.ruleup.verification.data.dto.StreakResponse
import com.ruleup.verification.data.dto.SubmitAppealRequest
import com.ruleup.verification.data.dto.SyncEnvelopeRequest
import com.ruleup.verification.data.dto.SyncResponse
import com.ruleup.verification.data.dto.UpdateMyLocationRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsResponse
import com.ruleup.verification.domain.entity.AlreadyVerifiedException
import com.ruleup.verification.domain.entity.AnchorSet
import com.ruleup.verification.domain.entity.AppealNotFailedException
import com.ruleup.verification.domain.entity.AppealTrack
import com.ruleup.verification.domain.entity.AppealWindowClosedException
import com.ruleup.verification.domain.entity.CancelWindowClosedException
import com.ruleup.verification.domain.entity.InvalidAnchorException
import com.ruleup.verification.domain.entity.InvalidAppealReasonException
import com.ruleup.verification.domain.entity.InvalidTargetDateException
import com.ruleup.verification.domain.entity.LocationLockedInWindowException
import com.ruleup.verification.domain.entity.LocationPin
import com.ruleup.verification.domain.entity.SettingChangeLimitException
import com.ruleup.verification.domain.entity.TodayResultStatus
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerificationRepositoryImplTest {
    @Test
    fun `수동 제출 409 ALREADY_VERIFIED 는 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(manualError = ErrorBody("ALREADY_VERIFIED", "이미 인증함"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

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
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

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
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<InvalidTargetDateException> { repository.submitManual("c1") }
        }

    @Test
    fun `자동 방 수동 제출은 도메인 어휘로 올리지 않는다`() =
        runTest {
            // 화면이 자동 방에 체크 버튼을 두지 않는 것이 전제다 — 도달하면 프로그래밍 오류다.
            val api = FakeVerificationApi(manualError = ErrorBody("NOT_MANUAL_CHALLENGE", "자동 방"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

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
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

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
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<LocationLockedInWindowException> { repository.updateMyLocation("c1", anchorSet()) }
        }

    @Test
    fun `월 1회 소진은 도메인 예외로 변환된다`() =
        runTest {
            val api = FakeVerificationApi(myLocationError = ErrorBody("SETTING_CHANGE_LIMIT", "소진"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<SettingChangeLimitException> { repository.updateMyLocation("c1", anchorSet()) }
        }

    @Test
    fun `앵커 개수 초과도 입력 수정 안내로 흐른다`() =
        runTest {
            // 클라가 먼저 막지만 서버가 되돌려주면 같은 인라인 안내를 쓴다.
            val api = FakeVerificationApi(myLocationError = ErrorBody("ANCHOR_LIMIT_EXCEEDED", "4개"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<InvalidAnchorException> { repository.updateMyLocation("c1", anchorSet()) }
        }

    @Test
    fun `판정 결과 확인은 해당 인증 건으로 호출된다`() =
        runTest {
            val api = FakeVerificationApi()
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            repository.acknowledgeResult("v_9911")

            assertEquals("v_9911", api.acknowledgedId)
        }

    @Test
    fun `수동 인증 취소 기한 경과는 도메인 예외로 변환된다`() =
        runTest {
            // 체크를 되돌릴 수 없는 이유를 화면이 말해 줘야 한다.
            val api = FakeVerificationApi(cancelError = ErrorBody("CANCEL_WINDOW_CLOSED", "기한 경과"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<CancelWindowClosedException> { repository.cancelManual("v_1") }
        }

    @Test
    fun `자동 판정 건 취소는 도메인 어휘로 올리지 않는다`() =
        runTest {
            // 화면이 자동 방에 취소 버튼을 두지 않는 것이 전제다 — 도달하면 프로그래밍 오류지 사용자 안내가 아니다.
            val api = FakeVerificationApi(cancelError = ErrorBody("NOT_MANUAL_VERIFICATION", "자동 건"))
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<ApiException> { repository.cancelManual("v_1") }
        }

    @Test
    fun `이의 형식 미달과 기한 경과와 이미 정정됨이 각각 다른 예외로 갈린다`() =
        runTest {
            // 화면이 셋을 다르게 말해야 한다 — 특히 NOT_FAILED 는 오류가 아니라 이미 정정된 건이다.
            val cases =
                listOf(
                    "INVALID_REASON" to InvalidAppealReasonException::class,
                    "APPEAL_WINDOW_CLOSED" to AppealWindowClosedException::class,
                    "NOT_FAILED" to AppealNotFailedException::class,
                )
            cases.forEach { (code, expected) ->
                val api = FakeVerificationApi(appealError = ErrorBody(code, code))
                val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

                val thrown = runCatching { repository.submitAppeal("v_1", "충분히 긴 사유입니다") }.exceptionOrNull()

                assertEquals(expected, thrown!!::class, "$code 매핑")
            }
        }

    @Test
    fun `이의 사진 업로드는 URL 만 돌려준다`() =
        runTest {
            val api = FakeVerificationApi()
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            val url = repository.uploadAppealImage("content://media/1")

            assertEquals("https://cdn.ruleup.co.kr/appeals/1.jpg", url)
            // 서버가 받는 파트 이름은 계약이다 — 다른 이름으로 보내면 오류도 없이 그냥 무시된다.
            assertTrue(
                api.uploadedPart
                    ?.headers
                    ?.get("Content-Disposition")
                    .orEmpty()
                    .contains("name=\"image\""),
            )
        }

    @Test
    fun `URL 이 없는 업로드 응답은 실패로 흐른다`() =
        runTest {
            // 화면이 빈 문자열을 imageUrl 로 실어 제출하면 서버가 깨진 링크를 저장한다.
            val api = FakeVerificationApi(uploadedImageUrl = null)
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            assertFailsWith<ApiException> { repository.uploadAppealImage("content://media/1") }
        }

    @Test
    fun `이의 이력은 최신순 그대로 매핑되고 식별자 없는 행은 버린다`() =
        runTest {
            // 한 행이 망가졌다고 현황 화면 전체가 비면 안 된다.
            val api =
                FakeVerificationApi(
                    myAppeals =
                        MyAppealsResponse(
                            history =
                                listOf(
                                    AppealHistoryItemResponse(
                                        appealId = "ap_301",
                                        date = "2026-07-20",
                                        challengeId = "c_301",
                                        routineTitle = "기상 인증",
                                        reason = "지하철에서 GPS가 끊겨...",
                                        track = "B",
                                    ),
                                    AppealHistoryItemResponse(appealId = null, date = "2026-07-12"),
                                ),
                        ),
                )
            val repository = VerificationRepositoryImpl(api, FakeKakaoLocalApi(), FakeImageReader())

            val history = repository.getMyAppeals()

            assertEquals(1, history.size)
            assertEquals("ap_301", history.single().appealId)
            assertEquals("기상 인증", history.single().routineTitle)
            assertEquals(AppealTrack.B, history.single().track)
        }

    private class FakeImageReader : ImageReader {
        override suspend fun read(uri: String): ImageBytes = ImageBytes(bytes = byteArrayOf(1, 2, 3), mimeType = "image/jpeg")
    }

    private fun anchorSet(): AnchorSet = AnchorSet.of(listOf(LocationPin(lat = 37.4979, lng = 127.0276, label = "새 헬스장")))

    private class FakeVerificationApi(
        private val manualSuccess: ManualSubmitResponse? = null,
        private val manualError: ErrorBody? = null,
        private val myLocationSuccess: MyLocationResponse? = null,
        private val myLocationError: ErrorBody? = null,
        private val cancelError: ErrorBody? = null,
        private val uploadedImageUrl: String? = "https://cdn.ruleup.co.kr/appeals/1.jpg",
        private val myAppeals: MyAppealsResponse? = null,
        private val appealError: ErrorBody? = null,
    ) : VerificationApi {
        var acknowledgedId: String? = null
        var uploadedPart: MultipartBody.Part? = null

        override suspend fun uploadAppealImage(image: MultipartBody.Part): BaseResponse<AppealImageResponse> {
            uploadedPart = image
            return BaseResponse(success = true, data = AppealImageResponse(imageUrl = uploadedImageUrl), error = null)
        }

        override suspend fun getMyAppeals(): BaseResponse<MyAppealsResponse> = BaseResponse(success = true, data = myAppeals, error = null)

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
            request: SubmitAppealRequest,
        ): BaseResponse<AppealResponse> =
            if (appealError != null) {
                BaseResponse(success = false, data = null, error = appealError)
            } else {
                BaseResponse(success = true, data = AppealResponse(appealId = "ap_1"), error = null)
            }
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
