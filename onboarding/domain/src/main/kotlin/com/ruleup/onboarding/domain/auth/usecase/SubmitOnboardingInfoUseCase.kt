package com.ruleup.onboarding.domain.auth.usecase

import com.ruleup.onboarding.domain.profile.OnboardingProfileRepository
import javax.inject.Inject

/**
 * 가입 기본정보(생년월일·성별) 제출 (명세 PUT /onboarding/me).
 *
 * 둘 다 선택이며 미입력은 null 로 생략된다. 인증이 필요하므로 가입 완료(토큰 확보) 후 호출한다.
 * 추천 개인화 보조 정보라 실패해도 온보딩 진행을 막지 않는다(호출부에서 흡수).
 */
class SubmitOnboardingInfoUseCase
    @Inject
    constructor(
        private val onboardingProfileRepository: OnboardingProfileRepository,
    ) {
        suspend operator fun invoke(
            birthDate: String?,
            gender: String?,
        ) {
            if (birthDate == null && gender == null) return
            onboardingProfileRepository.updateOnboardingInfo(birthDate = birthDate, gender = gender)
        }
    }
