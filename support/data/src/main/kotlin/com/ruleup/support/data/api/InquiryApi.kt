package com.ruleup.support.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.support.data.dto.InquiryDetailResponse
import com.ruleup.support.data.dto.InquiryImageResponse
import com.ruleup.support.data.dto.InquiryListResponse
import com.ruleup.support.data.dto.InquiryReceiptResponse
import com.ruleup.support.data.dto.InquiryRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface InquiryApi {
    // 문의 접수(201). base(.../api/) + v1/inquiries → /api/v1/inquiries
    // 계정 잠금 중에도 201 이 난다 — 서버 화이트리스트에 이 경로가 들어 있다(제재 정책 §5.3).
    @POST("v1/inquiries")
    suspend fun submit(
        @Body request: InquiryRequest,
    ): BaseResponse<InquiryReceiptResponse>

    // 내 문의 목록(최신순, 페이징 없음). → /api/v1/inquiries
    @GET("v1/inquiries")
    suspend fun getInquiries(): BaseResponse<InquiryListResponse>

    // 문의 1건. 남의 문의는 404 다. → /api/v1/inquiries/{inquiryId}
    @GET("v1/inquiries/{inquiryId}")
    suspend fun getInquiry(
        @Path("inquiryId") inquiryId: String,
    ): BaseResponse<InquiryDetailResponse>

    /**
     * 첨부 사진 업로드. → /api/v1/appeals/images
     *
     * **문의 전용 업로드 경로가 명세에 없어 이의 제기 업로드를 재사용한다**(2026-09-11 합의).
     * 서버가 용도별 소유권을 검증하기 시작하면 접수에서 거절되므로, 그때 경로만 갈아 끼운다.
     */
    @Multipart
    @POST("v1/appeals/images")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<InquiryImageResponse>
}
