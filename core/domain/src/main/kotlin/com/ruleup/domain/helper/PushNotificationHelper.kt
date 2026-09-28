package com.ruleup.domain.helper

import com.ruleup.domain.navigation.NavRoute

/** OS 시스템 알림(트레이)을 띄우는 포트. */
interface PushNotificationHelper {
    /** 탭하면 [route] 화면으로 진입한다. */
    fun show(
        id: Int,
        title: String,
        message: String,
        route: NavRoute,
    )
}
