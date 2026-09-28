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
    // 문의 접수(201).
    @POST("v1/inquiries")
    suspend fun submit(
        @Body request: InquiryRequest,
    ): BaseResponse<InquiryReceiptResponse>

    // 내 문의 목록(최신순, 페이징 없음).
    @GET("v1/inquiries")
    suspend fun getInquiries(): BaseResponse<InquiryListResponse>

    // 문의 1건.
    @GET("v1/inquiries/{inquiryId}")
    suspend fun getInquiry(
        @Path("inquiryId") inquiryId: String,
    ): BaseResponse<InquiryDetailResponse>

    /** 첨부 사진 업로드. */
    @Multipart
    @POST("v1/appeals/images")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part,
    ): BaseResponse<InquiryImageResponse>
}
