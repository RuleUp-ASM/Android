package com.ruleup.notification.domain.navigation

import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
import com.ruleup.domain.navigation.Page

/** 알림 센터 (마이 허브 · 홈 벨 아이콘 → 알림함). */
data object NotificationCenterPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.NOTIFICATIONS
}

/** 알림 설정 (설정 허브 → 알림). */
data object NotificationSettingsPage : Page {
    override fun toRoute(): NavRoute = NavRoute(PATH)

    const val PATH = AppRoutes.NOTIFICATION_SETTINGS
}
