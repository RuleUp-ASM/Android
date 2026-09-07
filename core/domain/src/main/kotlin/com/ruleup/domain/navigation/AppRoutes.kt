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
 * 감시자 초대 수락(`/w/{token}`)은 [CHALLENGE_WATCHER_ACCEPT] 가 받는다 — 웹 동의가 폐지되고
 * **인앱 수락만 동의로 인정**되도록 바뀌었다(감시자 테크 스펙 5-2·2026-08-31).
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
    const val CHALLENGE_LIST = "challenge/list" // 진입점 (하단 탭 → 내 챌린지: 진행 중 / 완료·이탈)
    const val CHALLENGE_EXPLORE = "challenge/explore" // 진입점 (하단 탭 → 탐색 메인)
    const val CHALLENGE_EXPLORE_LIST = "challenge/explore/list" // 챌린지 둘러보기(필터+정렬 목록)
    const val CHALLENGE_RANKING = "challenge/ranking" // 그룹 랭킹(방 홈 → 랭킹)
    const val CHALLENGE_INVITE = "challenge/invite" // 진입점 (멤버 초대 링크 /c/{token})
    const val CHALLENGE_WATCHER_ACCEPT = "challenge/watcher/accept" // 진입점 (감시자 초대 링크 /w/{token})
    const val CHALLENGE_SETTINGS = "challenge/settings" // 챌린지 수정(방장 전용, 방 설정 → 수정)

    // profile (마이)
    const val MY_HOME = "my/home" // 진입점 (하단 MY 탭 → 마이 홈)
    const val MY_TIER = "my/tier" // 내 티어 상세 (점수·구간·최근 변동)
    const val MY_TIER_HISTORY = "my/tier/history" // 티어 히스토리 (월말 스냅샷·역대 최고)
    const val MY_CALENDAR = "my/calendar" // 활동 캘린더
    const val MY_APPEALS = "my/appeals" // 이의 내역 (내가 낸 이의)
    const val MY_STATS = "my/stats" // 통계 리포트
    const val MY_INVITE = "my/invite" // 친구 초대
    const val MY_PROFILE_EDIT = "my/profile/edit" // 프로필 편집(마이 → 재편집)
    const val MY_BLOCKS = "my/blocks" // 신고한 사용자·챌린지 (마이 → 차단 목록·해제)
    const val MY_SETTINGS = "my/settings" // 설정 허브 (마이 → 계정·약관)
    const val MY_WATCHING = "my/watching" // 패널티 수신 관리 (내가 감시자로 지정된 관계)
    const val MY_AGREEMENTS = "my/agreements" // 약관·개인정보 동의 관리
    const val MY_SANCTIONS = "my/sanctions" // 제재 통지·이력 (잠금 상태에서도 열려야 한다)

    // notification
    const val NOTIFICATIONS = "notifications" // 진입점 (홈 벨·마이 → 알림 센터)
    const val NOTIFICATION_SETTINGS = "notifications/settings" // 설정 허브 → 알림 설정

    // verification
    const val VERIFICATION_PERMISSION_REPAIR = "verification/permission-repair" // 진입점 (권한 회수 복구)
    const val VERIFICATION_LOCATION = "verification/location" // 진입점 (지도 핀 → 지오펜스 좌표 바인딩)
}
