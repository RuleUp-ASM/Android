package com.ruleup.onboarding.data.auth.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.onboarding.data.auth.dto.LogoutRequest
import com.ruleup.onboarding.data.auth.dto.SignUpRequest
import com.ruleup.onboarding.data.auth.dto.SignUpResponse
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthRequest
import com.ruleup.onboarding.data.auth.dto.SocialLoginAuthResponse
import com.ruleup.onboarding.data.auth.dto.TokenRefreshRequest
import com.ruleup.onboarding.data.auth.dto.TokenRefreshResponse
import com.ruleup.onboarding.data.auth.dto.WithdrawRequest
import com.ruleup.onboarding.data.auth.dto.WithdrawResponse
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApi {
    // OAuth 로그인 (provider: kakao / google).
    @POST("v1/auth/oauth/{provider}")
    suspend fun socialLogin(
        @Path("provider") provider: String,
        @Body request: SocialLoginAuthRequest,
    ): BaseResponse<SocialLoginAuthResponse>

    // 회원가입 완료.
    @POST("v1/auth/signup")
    suspend fun signup(
        @Body request: SignUpRequest,
    ): BaseResponse<SignUpResponse>

    // 앱 토큰 재발급.
    @POST("v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: TokenRefreshRequest,
    ): BaseResponse<TokenRefreshResponse>

    // 로그아웃 (refreshToken revoke).
    @POST("v1/auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): BaseResponse<EmptyData>

    // 회원 탈퇴.
    @HTTP(method = "DELETE", path = "v1/users/me", hasBody = true)
    suspend fun withdraw(
        @Body request: WithdrawRequest,
    ): BaseResponse<WithdrawResponse>
}
