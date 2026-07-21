package com.ruleup.onboarding.presentation.profile.viewmodel

/** 가입 기본정보 성별 (PUT /onboarding/me gender). 미선택은 null(응답 안 함). */
enum class OnboardingGender(
    val value: String,
) {
    MALE("MALE"),
    FEMALE("FEMALE"),
}
