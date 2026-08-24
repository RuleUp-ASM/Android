package com.ruleup.profile.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 매너 온도 상세 페이지 (마이 홈 메뉴 → 현재 온도·다음 구간 진행·최근 변동). */
data object MyTemperaturePage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_TEMPERATURE
}

/** 평판 히스토리 페이지 (온도 상세 → 역대 최고·마일스톤 피드). */
data object ReputationHistoryPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.MY_REPUTATION_HISTORY
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
