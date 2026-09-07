package com.ruleup.profile.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.profile.data.dto.AgreementStatusResponse
import com.ruleup.profile.data.dto.AgreementSubmitRequest
import com.ruleup.profile.data.dto.SanctionHistoryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * 계정 설정 — 동의와 제재.
 *
 * `MyPageApi`(마이 탭 조회 대시보드)·`ProfileApi`(신원·편집)와 갈라 두는 이유는 이 둘이
 * **잠금 상태에서의 접근 규칙이 다른** 계정 관리 영역이기 때문이다.
 */
interface AccountApi {
    // 동의 현황 조회 — 약관 5종 + 개별 동의 2종
    @GET("v1/users/me/agreements")
    suspend fun getAgreements(): BaseResponse<AgreementStatusResponse>

    // 동의 제출·철회. 여러 항목을 한 트랜잭션으로 처리한다
    @POST("v1/users/me/agreements")
    suspend fun submitAgreements(
        @Body request: AgreementSubmitRequest,
    ): BaseResponse<AgreementStatusResponse>

    // 제재 통지·이력. 잠금 계정도 열려야 하는 화이트리스트 API 다
    @GET("v1/users/me/sanctions")
    suspend fun getSanctions(): BaseResponse<SanctionHistoryResponse>
}
