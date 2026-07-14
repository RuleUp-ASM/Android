package com.ruleup.domain.navigation

/**
 * 앱의 모든 화면 path 단일 소스(single source of truth).
 *
 * feature 의 Page 객체는 path 문자열을 직접 갖지 않고 여기 상수를 참조한다.
 * (Page 객체와 인자 헬퍼는 각 feature domain 에 유지된다.)
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
    const val CHALLENGE_DETAIL = "challenge/detail" // 진입점 (홈 카드 → 챌린지 상세/참여)
    const val CHALLENGE_TARGETS = "challenge/targets" // 대상 앱 등록(상세 → 앱 등록하기)
    const val CHALLENGE_EXPLORE = "challenge/explore" // 진입점 (하단 탭 → 탐색 메인)
    const val CHALLENGE_EXPLORE_LIST = "challenge/explore/list" // 챌린지 둘러보기(필터+정렬 목록)
    const val WATCHER_INVITATION = "watchers/invitation" // 진입점 (감시자 초대 링크 → 인앱 수락)

    // verification
    const val VERIFICATION_PROGRESS = "verification/progress" // 진입점 (내 챌린지 진행률 일괄)
    const val VERIFICATION_DETAIL = "verification/detail" // 진입점 (홈 카드 → 검증 결과/실패)
    const val VERIFICATION_LOCATION = "verification/location" // 진입점 (지도 핀 → 지오펜스 좌표 바인딩)
    const val VERIFICATION_MANUAL = "verification/manual" // 진입점 (수동 인증 제출, VF-04)
}
