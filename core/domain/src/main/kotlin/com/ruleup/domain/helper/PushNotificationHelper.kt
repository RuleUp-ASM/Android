package com.ruleup.domain.helper

import com.ruleup.domain.navigation.NavRoute

/**
 * OS 시스템 알림(트레이)을 띄우는 포트. 인앱 토스트/스낵바([MessageHelper])와 달리 앱이 백그라운드여도 보인다.
 * 목적지는 URL 이 아니라 [NavRoute] 로 넘긴다 — 앱이 자기 액티비티를 직접 열어 매니페스트 필터가 필요 없다.
 */
interface PushNotificationHelper {
    /** 탭하면 [route] 화면으로 진입한다. 같은 [id] 는 갱신되고, POST_NOTIFICATIONS(33+) 미허용이면 무시된다. */
    fun show(
        id: Int,
        title: String,
        message: String,
        route: NavRoute,
    )
}
