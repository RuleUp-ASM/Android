package com.ruleup.domain.navigation

/**
 * 앱의 모든 화면 path 단일 소스(single source of truth).
 *
 * feature 의 Page 객체는 path 문자열을 직접 갖지 않고 여기 상수를 참조한다.
 * 이렇게 모든 경로를 한곳에서 관리해 오타·중복을 방지하고 전체 라우트 네임스페이스를
 * 한눈에 본다. (Page 객체와 인자 헬퍼는 각 feature domain 에 유지된다.)
 *
 * 주석의 "진입점" 표시는 다른 feature 가 직접 이동해 들어오는 cross-feature 공개 경로다
 * (예: 챌린지 생성 완료 → [HOME], 홈의 챌린지 생성 버튼 → [CHALLENGE_CREATE]).
 */
object AppRoutes {
    // onboarding
    const val SPLASH = "splash"
    const val INTRO_PROMISE = "intro/promise"
    const val INTRO_VERIFY = "intro/verify"
    const val INTRO_TRUST = "intro/trust"
    const val LOGIN = "login"
    const val PROFILE_ICON = "profile/icon"
    const val PROFILE_NICKNAME = "profile/nickname"
    const val PROFILE_INTEREST = "profile/interest"
    const val PROFILE_PERMISSION = "profile/permission"
    const val PROFILE_AGREEMENT = "profile/agreement"
    const val HOME = "home" // 진입점

    // challenge
    const val CHALLENGE_CREATE = "challenge/create" // 진입점
    const val CHALLENGE_CONFIRM = "challenge/confirm"
}
