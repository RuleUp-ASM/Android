package com.ruleup.onboarding.data.profile.api

import com.ruleup.network.dto.BaseResponse
import com.ruleup.network.dto.EmptyData
import com.ruleup.onboarding.data.profile.dto.OnboardingMeRequest
import retrofit2.http.Body
import retrofit2.http.PUT

interface OnboardingProfileApi {
    // 가입 기본정보 입력 (PUT /onboarding/me — birthDate·gender 선택, 추천 세그먼트용)
    @PUT("v1/onboarding/me")
    suspend fun updateOnboardingMe(
        @Body request: OnboardingMeRequest,
    ): BaseResponse<EmptyData>
}
