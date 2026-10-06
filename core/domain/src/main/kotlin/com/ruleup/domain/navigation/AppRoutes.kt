package com.ruleup.domain.navigation

/** 앱의 모든 화면 path 단일 소스(single source of truth). */
object AppRoutes {
    // onboarding
    const val SPLASH = "splash"

    /** 첫 실행 워크쓰루 3장. */
    const val WALKTHROUGH = "walkthrough"
    const val LOGIN = "login"

    // 가입 온보딩 6단계.
    const val ONBOARDING_NICKNAME = "onboarding/nickname"
    const val ONBOARDING_INTEREST = "onboarding/interest"
    const val ONBOARDING_BIRTH = "onboarding/birth"
    const val ONBOARDING_GENDER = "onboarding/gender"
    const val ONBOARDING_PHOTO = "onboarding/photo"
    const val ONBOARDING_TERMS = "onboarding/terms"

    // 약관 원문 열람.
    const val TERMS_DOCUMENT = "terms/document"
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
    const val CHALLENGE_WATCHERS = "challenge/watchers" // 내 감시자 관리(마이 → 감시자 → 방 선택)

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

    /** 타인 프로필 (방 멤버·랭킹 행 → 프로필). */
    const val MEMBER_PROFILE = "profile/member"

    /** 잠금 화면. */
    const val ACCOUNT_LOCKED = "account/locked"

    // report
    const val REPORT = "report" // 진입점 (타인 프로필·방 상세 → 신고하기)

    // notification
    const val NOTIFICATIONS = "notifications" // 진입점 (홈 벨·마이 → 알림 센터)
    const val NOTIFICATION_SETTINGS = "notifications/settings" // 설정 허브 → 알림 설정

    // support
    const val MY_INQUIRIES = "my/inquiries" // 설정 허브 → 내 문의 내역
    const val MY_INQUIRY_NEW = "my/inquiries/new" // 설정 허브 → 문의하기 (분류 선택)
    const val MY_INQUIRY_COMPOSE = "my/inquiries/compose" // 분류 선택 → 본문 작성
    const val MY_INQUIRY_DETAIL = "my/inquiries/detail" // 내역 → 문의 1건 열람

    // verification
    const val VERIFICATION_PERMISSION_REPAIR = "verification/permission-repair" // 진입점 (권한 회수 복구)
    const val VERIFICATION_MANUAL = "verification/manual" // 진입점 (수동 방 → 오늘 인증 체크)
    const val VERIFICATION_LOCATION = "verification/location" // 진입점 (지도 핀 → 지오펜스 좌표 바인딩)
}
