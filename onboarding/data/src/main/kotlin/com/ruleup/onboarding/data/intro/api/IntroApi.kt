package com.ruleup.onboarding.data.intro.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.onboarding.data.intro.dto.IntroResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface IntroApi {
    /**
     * 앱 버전 게이트(공개, 토큰 불필요). 헤더 appVersionCode 로 강제/권장 업데이트를 판정한다.
     * GET 이라 본문 대신 platform·versionName 은 쿼리로 보낸다(서버는 헤더만 검증).
     */
    @GET("v1/intro")
    suspend fun getIntro(
        @Header("appVersionCode") appVersionCode: Int,
        @Query("platform") platform: String,
        @Query("versionName") versionName: String,
    ): BaseResponse<IntroResponse>
}
