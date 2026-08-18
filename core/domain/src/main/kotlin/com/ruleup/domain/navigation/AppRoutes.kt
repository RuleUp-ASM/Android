package com.ruleup.domain.navigation

/**
 * 앱의 모든 화면 path 단일 소스(single source of truth).
 *
 * feature 의 Page 객체는 path 문자열을 직접 갖지 않고 여기 상수를 참조한다.
 * (Page 객체와 인자 헬퍼는 각 feature domain 에 유지된다.)
 *
 * 주석의 "진입점" 표시는 다른 feature 가 직접 이동해 들어오는 cross-feature 공개 경로다
 * (예: 챌린지 생성 완료 → [HOME], 홈의 챌린지 생성 버튼 → [CHALLENGE_CREATE]).
 *
 * 감시자 초대 수락(`/w/{token}`)은 여기 없다 — 비유저 감시자를 포함해 웹 동의 페이지가 담당하므로
 * 앱에는 대응하는 화면도 경로도 두지 않는다.
 */
object AppRoutes {
    // onboarding
    const val SPLASH = "splash"
    const val LOGIN = "login"

    // 가입 온보딩 6단계. 순서가 계약이다 — 서버가 생일·성별을 필수로 받고 약관이 마지막이다.
    const val ONBOARDING_NICKNAME = "onboarding/nickname"
    const val ONBOARDING_INTEREST = "onboarding/interest"
    const val ONBOARDING_BIRTH = "onboarding/birth"
    const val ONBOARDING_GENDER = "onboarding/gender"
    const val ONBOARDING_PHOTO = "onboarding/photo"
    const val ONBOARDING_TERMS = "onboarding/terms"
    const val HOME = "home" // 진입점

    // challenge
    const val CHALLENGE_CREATE = "challenge/create" // 진입점
    const val CHALLENGE_CONFIRM = "challenge/confirm"
    const val CHALLENGE_DETAIL = "challenge/detail" // 진입점 (홈 카드 → 챌린지 상세/참여)
    const val CHALLENGE_TARGETS = "challenge/targets" // 대상 앱 등록(상세 → 앱 등록하기)
    const val CHALLENGE_EXPLORE = "challenge/explore" // 진입점 (하단 탭 → 탐색 메인)
    const val CHALLENGE_EXPLORE_LIST = "challenge/explore/list" // 챌린지 둘러보기(필터+정렬 목록)
    const val CHALLENGE_RANKING = "challenge/ranking" // 그룹 랭킹(방 홈 → 랭킹)
    const val CHALLENGE_SETTINGS = "challenge/settings" // 챌린지 수정(방장 전용, 방 설정 → 수정)

    // profile (마이)
    const val MY_HOME = "my/home" // 진입점 (하단 MY 탭 → 마이 홈)
    const val MY_TEMPERATURE = "my/temperature" // 매너 온도 상세
    const val MY_REPUTATION_HISTORY = "my/reputation/history" // 평판 히스토리
    const val MY_CALENDAR = "my/calendar" // 활동 캘린더
    const val MY_STATS = "my/stats" // 통계 리포트
    const val MY_INVITE = "my/invite" // 친구 초대
    const val MY_PROFILE_EDIT = "my/profile/edit" // 프로필 편집(마이 → 재편집)

    // verification
    const val VERIFICATION_PROGRESS = "verification/progress" // 진입점 (내 챌린지 진행률 일괄)
    const val VERIFICATION_DETAIL = "verification/detail" // 진입점 (홈 카드 → 검증 결과/실패)
    const val VERIFICATION_LOCATION = "verification/location" // 진입점 (지도 핀 → 지오펜스 좌표 바인딩)
    const val VERIFICATION_MANUAL = "verification/manual" // 진입점 (수동 인증 제출, VF-04)
    const val VERIFICATION_PENDING_REVIEWS = "verification/pending-reviews" // 진입점 (방장·관리자 확인 대기함)
}
