package com.ruleup.profile.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.profile.data.dto.AgreementStatusResponse
import com.ruleup.profile.data.dto.AgreementSubmitRequest
import com.ruleup.profile.data.dto.SanctionHistoryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/** 계정 설정 */
interface AccountApi {
    // 동의 현황 조회
    @GET("v1/users/me/agreements")
    suspend fun getAgreements(): BaseResponse<AgreementStatusResponse>

    // 동의 제출·철회.
    @POST("v1/users/me/agreements")
    suspend fun submitAgreements(
        @Body request: AgreementSubmitRequest,
    ): BaseResponse<AgreementStatusResponse>

    // 제재 통지·이력.
    @GET("v1/users/me/sanctions")
    suspend fun getSanctions(): BaseResponse<SanctionHistoryResponse>
}
