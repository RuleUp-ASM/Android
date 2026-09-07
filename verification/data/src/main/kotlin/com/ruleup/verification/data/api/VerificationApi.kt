package com.ruleup.verification.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.verification.data.dto.AcknowledgeResponse
import com.ruleup.verification.data.dto.AppealImageResponse
import com.ruleup.verification.data.dto.AppealResponse
import com.ruleup.verification.data.dto.CancelManualResponse
import com.ruleup.verification.data.dto.ChallengeSetupRequest
import com.ruleup.verification.data.dto.ChallengeSetupResponse
import com.ruleup.verification.data.dto.IntroRequest
import com.ruleup.verification.data.dto.IntroResponse
import com.ruleup.verification.data.dto.ManualSubmitRequest
import com.ruleup.verification.data.dto.ManualSubmitResponse
import com.ruleup.verification.data.dto.MyAppealsResponse
import com.ruleup.verification.data.dto.MyLocationResponse
import com.ruleup.verification.data.dto.MyScreenAppsResponse
import com.ruleup.verification.data.dto.ProgressResponse
import com.ruleup.verification.data.dto.SubmitAppealRequest
import com.ruleup.verification.data.dto.SyncEnvelopeRequest
import com.ruleup.verification.data.dto.SyncResponse
import com.ruleup.verification.data.dto.TodayResultResponse
import com.ruleup.verification.data.dto.UpdateMyLocationRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsRequest
import com.ruleup.verification.data.dto.UpdateScreenAppsResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface VerificationApi {
    // Phase 0 인트로 (전송 스펙 §0.3): 정적 프로필 + 최초 권한 스냅샷 → 서버 정책
    @POST("v1/verifications/intro")
    suspend fun intro(
        @Body request: IntroRequest,
    ): BaseResponse<IntroResponse>

    // 3.1 30분마다 신호 받고 평가 (전송 스펙 §0.1 envelope)
    @POST("v1/verifications/sync")
    suspend fun sync(
        @Body request: SyncEnvelopeRequest,
    ): BaseResponse<SyncResponse>

    // 3.2 챌린지 진행률 일괄 조회
    @GET("v1/verifications/progress")
    suspend fun getProgress(
        @Query("status") status: String? = null,
    ): BaseResponse<ProgressResponse>

    // 오늘 인증 결과 + 판정 결과 모달 데이터. 구 GET /{id}/verification 을 대체하는 계약이다.
    @GET("v1/challenges/{challengeId}/verifications/today")
    suspend fun getTodayResult(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<TodayResultResponse>

    // 3.4 수동 인증 제출 (VF-04)
    @POST("v1/challenges/{challengeId}/verifications")
    suspend fun submitManual(
        @Path("challengeId") challengeId: String,
        @Body request: ManualSubmitRequest,
    ): BaseResponse<ManualSubmitResponse>

    // setup 앵커·대상앱 바인딩 제출 (명세 setup)
    @POST("v1/challenges/{challengeId}/setup")
    suspend fun setup(
        @Path("challengeId") challengeId: String,
        @Body request: ChallengeSetupRequest,
    ): BaseResponse<ChallengeSetupResponse>

    // 앵커 조회 (GET /my-location): 셋업/수정 재진입 시 내 인증 장소 핀 복원
    @GET("v1/challenges/{challengeId}/my-location")
    suspend fun getMyLocation(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<MyLocationResponse>

    // 앵커 세트 교체 (PUT /my-location): 월 1회, 인증 윈도우 중에는 409 로 거부된다
    @PUT("v1/challenges/{challengeId}/my-location")
    suspend fun updateMyLocation(
        @Path("challengeId") challengeId: String,
        @Body request: UpdateMyLocationRequest,
    ): BaseResponse<MyLocationResponse>

    // 스크린타임 대상 앱 조회 (GET /my-screen-apps): 셋업/수정 재진입 시 이전 선택 복원
    @GET("v1/challenges/{challengeId}/my-screen-apps")
    suspend fun getMyScreenApps(
        @Path("challengeId") challengeId: String,
    ): BaseResponse<MyScreenAppsResponse>

    // 스크린타임 대상 앱 세트 교체 (PUT /my-screen-apps): 익일 00:00 부터 적용
    @PUT("v1/challenges/{challengeId}/my-screen-apps")
    suspend fun updateMyScreenApps(
        @Path("challengeId") challengeId: String,
        @Body request: UpdateScreenAppsRequest,
    ): BaseResponse<UpdateScreenAppsResponse>

    // 판정 결과 확인 (POST ack): 결과 모달을 봤다는 표시. 멱등이라 중복 호출이 안전하다
    @POST("v1/verifications/{verificationId}/ack")
    suspend fun acknowledgeResult(
        @Path("verificationId") verificationId: String,
    ): BaseResponse<AcknowledgeResponse>

    // 수동 인증 취소 (DELETE): 당일(KST) 안에서만. 자동 판정 건은 취소 불가
    @DELETE("v1/verifications/{verificationId}")
    suspend fun cancelManual(
        @Path("verificationId") verificationId: String,
    ): BaseResponse<CancelManualResponse>

    // 이의 증빙 사진 업로드 (POST /appeals/images): 반환 URL 을 이의 제출 body 에 넣는다
    @Multipart
    @POST("v1/appeals/images")
    suspend fun uploadAppealImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<AppealImageResponse>

    // 내가 낸 이의 이력 (GET /users/me/appeals): 마이 허브의 이의 제기 현황
    @GET("v1/users/me/appeals")
    suspend fun getMyAppeals(): BaseResponse<MyAppealsResponse>

    // 인증 이의 제기 (POST appeals): 판정 없이 형식 요건만 보고 자동 인용
    @POST("v1/verifications/{verificationId}/appeals")
    suspend fun submitAppeal(
        @Path("verificationId") verificationId: String,
        @Body request: SubmitAppealRequest,
    ): BaseResponse<AppealResponse>
}
