package com.ruleup.onboarding.data.profile.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 가입 기본정보 입력 (PUT /onboarding/me — 둘 다 선택, 미입력은 null 로 생략)
@Serializable
data class OnboardingMeRequest(
    @SerialName("birthDate")
    val birthDate: String? = null,
    @SerialName("gender")
    val gender: String? = null,
)
