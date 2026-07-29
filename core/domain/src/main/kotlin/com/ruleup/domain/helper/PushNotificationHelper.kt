package com.ruleup.domain.helper

import com.ruleup.domain.navigation.NavRoute

/**
 * OS 시스템 알림(트레이)을 띄우는 포트. 인앱 토스트/스낵바([MessageHelper])와 책임이 다르다 —
 * 앱이 백그라운드여도 보이고, 탭하면 앱의 목적지 화면으로 진입한다.
 *
 * 구현은 app 이 채운다. 알림은 앱이 직접 만들어 자기 액티비티를 여는 것이라 **URL 을 경유하지 않는다** —
 * 목적지를 [NavRoute] 로 그대로 넘긴다.
 */
interface PushNotificationHelper {
    /**
     * 알림을 띄운다. 탭하면 [route] 화면으로 진입한다.
     * 같은 [id] 는 갱신되며, POST_NOTIFICATIONS(33+) 미허용이면 무시된다.
     */
    fun show(
        id: Int,
        title: String,
        message: String,
        route: NavRoute,
    )
}
