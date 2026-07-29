package com.ruleup.onboarding.domain.profile

/**
 * 가입 기본정보 입력(명세: PUT /onboarding/me). 추천 세그먼트용 생년월일·성별.
 *
 * 계정 프로필 조회·수정은 profile 소관이라
 * [com.ruleup.profile.domain.repository.ProfileRepository] 를 빌려 쓰지만, 이 엔드포인트는
 * 가입 흐름에만 있어 온보딩이 소유한다.
 */
interface OnboardingProfileRepository {
    /** 둘 다 선택이며, 원치 않는 값은 null 로 전달해 전송을 생략한다. 가입 완료(토큰 확보) 후 호출한다. */
    suspend fun updateOnboardingInfo(
        birthDate: String? = null,
        gender: String? = null,
    )
}
