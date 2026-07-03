package com.ruleup.domain.helper

/**
 * OS 시스템 알림(트레이)을 띄우는 포트. 인앱 토스트/스낵바([MessageHelper])와 책임이 다르다 —
 * 앱이 백그라운드여도 보이고, 탭하면 딥링크로 앱에 진입한다.
 *
 * 구현은 플랫폼(NotificationManager) 의존이라 core:ui 가 채운다.
 */
interface PushNotificationHelper {
    /**
     * 알림을 띄운다. 탭하면 [deepLink](예: `ruleup://app/challenge/detail?challengeId=…`)로 진입한다.
     * 같은 [id] 는 갱신되며, POST_NOTIFICATIONS(33+) 미허용이면 무시된다.
     */
    fun show(
        id: Int,
        title: String,
        message: String,
        deepLink: String,
    )
}
