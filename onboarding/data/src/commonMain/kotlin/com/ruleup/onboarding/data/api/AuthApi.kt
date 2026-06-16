package com.ruleup.onboarding.data.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.onboarding.data.dto.LogoutRequest
import com.ruleup.onboarding.data.dto.SignUpRequest
import com.ruleup.onboarding.data.dto.SignUpResponse
import com.ruleup.onboarding.data.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.dto.SocialLoginAuthResponse
import com.ruleup.onboarding.data.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.dto.TokenRefreshResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.Path

interface AuthApi {
    // 4.1/4.2 소셜 로그인 (provider: kakao / google …)
    @POST("v1/account/login/{provider}")
    suspend fun socialLogin(
        @Path("provider") provider: String,
        @Body request: SocialLoginAuthRequest,
    ): BaseResponse<SocialLoginAuthResponse>

    // 4.3 신규 가입 완료
    @POST("v1/account/signup")
    suspend fun signup(
        @Body request: SignUpRequest,
    ): BaseResponse<SignUpResponse>

    // 4.4 앱 토큰 재발급
    @POST("v1/account/token/refresh")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest,
    ): BaseResponse<TokenRefreshResponse>

    // 4.5 로그아웃 (refreshToken revoke)
    @POST("v1/account/logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): BaseResponse<EmptyData>
}
