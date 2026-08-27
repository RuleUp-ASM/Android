package com.ruleup.onboarding.data.intro.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.onboarding.data.intro.dto.IntroResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface IntroApi {
    /**
     * 앱 진입 정보(공개, 토큰 불필요). `platform` 은 **헤더 필수**다 — 빠지면 400 `INVALID_REQUEST` 고,
     * 플랫폼마다 버전 체계·최소 버전이 달라 이 값 없이는 강제 업데이트를 판정할 수 없다.
     */
    @GET("v1/intro")
    suspend fun getIntro(
        @Header("appVersionCode") appVersionCode: Int,
        @Header("platform") platform: String,
    ): BaseResponse<IntroResponse>
}
