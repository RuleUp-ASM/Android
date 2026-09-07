package com.ruleup.profile.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 내 티어 페이지 (마이 홈 티어 카드 → 점수·구간표·최근 변동 10건). */
data object MyTierPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_TIER
}

/** 티어 히스토리 페이지 (내 티어 → 월말 스냅샷·역대 최고). */
data object MyTierHistoryPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_TIER_HISTORY
}

/** 활동 캘린더 페이지 (마이 홈 메뉴 → 월 단위 일자별 상태). */
data object MyCalendarPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_CALENDAR
}

/** 이의 내역 페이지 (마이 홈 메뉴 → 내가 낸 이의). */
data object MyAppealsPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_APPEALS
}

/** 통계 리포트 페이지 (마이 홈 메뉴 → 주간/월간/연간). */
data object MyStatsPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_STATS
}

/** 친구 초대 페이지 (마이 홈 메뉴 → 코드/링크/QR + 초대 현황). */
data object FriendInvitePage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_INVITE
}

/** 프로필 편집 페이지 (마이 홈 프로필 영역 탭 → 닉네임·카테고리·이미지 수정). */
data object ProfileEditPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_PROFILE_EDIT
}

/** 설정 허브 페이지 (마이 홈 → 계정 · 약관). */
data object MySettingsPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_SETTINGS
}

/** 약관 · 개인정보 동의 관리 페이지 (설정 허브 → 약관). */
data object MyAgreementsPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_AGREEMENTS
}

/**
 * 제재 통지·이력 페이지 (설정 허브 → 제재 이력).
 *
 * **잠금 상태에서도 열려야 한다** — 잠금 사유와 해제일을 볼 유일한 경로다.
 */
data object MySanctionsPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_SANCTIONS
}

/** 패널티 수신 관리 페이지 (설정 허브 → 내가 받는 알림). */
data object MyWatchingPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_WATCHING
}
