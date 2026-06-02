package com.ruleup.data.auth.api

import com.ruleup.data.auth.dto.request.AuthRequest
import com.ruleup.data.auth.dto.request.LogoutRequest
import com.ruleup.data.auth.dto.request.RefreshRequest
import com.ruleup.data.auth.dto.request.SignupRequest
import com.ruleup.data.auth.dto.response.AuthResponse
import com.ruleup.data.auth.dto.response.NicknameAvailabilityResponse
import com.ruleup.data.auth.dto.response.SignupResponse
import com.ruleup.data.auth.dto.response.TokenResponse
import com.ruleup.network.dto.BaseResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("auth/oauth/{provider}")
    suspend fun oauthLogin(
        @Path("provider") provider: String,
        @Body request: AuthRequest,
    ): BaseResponse<AuthResponse>

    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest,
    ): BaseResponse<SignupResponse>

    @POST("auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest,
    ): BaseResponse<TokenResponse>

    @POST("auth/logout")
    suspend fun logout(
        @Body request: LogoutRequest,
    ): BaseResponse<Unit>

    @GET("auth/nickname-availability")
    suspend fun checkNickname(
        @Query("nickname") nickname: String,
    ): BaseResponse<NicknameAvailabilityResponse>
}
