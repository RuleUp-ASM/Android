package com.ruleup.android_ruleup.observability

import com.ruleup.android_ruleup.logging.ScreenPathHolder
import com.ruleup.logging.domain.BizLogger
import com.ruleup.logging.domain.CommonBizEvents
import com.ruleup.observability.data.context.ScreenContextHolder
import com.ruleup.observability.domain.model.ScreenKey
import javax.inject.Inject
import javax.inject.Singleton

/** 화면 진입 추적. */
@Singleton
class ScreenTracker
    @Inject
    constructor(
        private val bizLogger: BizLogger,
        private val contextHolder: ScreenContextHolder,
        private val screenPathHolder: ScreenPathHolder,
        private val jankTracker: JankTracker,
    ) {
        private var current: String? = null

        fun onScreenEntered(path: String) {
            val from = current
            // 진행 중이던 jank 창을 여기서 확정한다.
            jankTracker.onScreenChanged()
            contextHolder.setScreen(ScreenKey(path))
            // 기록기가 홀더에서 화면을 읽어 가므로, 이벤트에 새 화면이 실리려면 기록보다 먼저 바꿔야 한다.
            screenPathHolder.setScreen(path)
            current = path
            bizLogger.record(CommonBizEvents.screenView(screen = path, from = from))
        }
    }
