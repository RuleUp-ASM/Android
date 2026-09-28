package com.ruleup.onboarding.data.intro.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.onboarding.data.intro.dto.IntroResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface IntroApi {
    /** 앱 진입 정보(공개, 토큰 불필요). */
    @GET("v1/intro")
    suspend fun getIntro(
        @Header("appVersionCode") appVersionCode: Int,
        @Header("platform") platform: String,
    ): BaseResponse<IntroResponse>
}
