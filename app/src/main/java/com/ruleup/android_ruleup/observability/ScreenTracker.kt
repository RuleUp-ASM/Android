package com.ruleup.android_ruleup.observability

import com.ruleup.android_ruleup.logging.ScreenPathHolder
import com.ruleup.logging.domain.BizLogger
import com.ruleup.logging.domain.CommonBizEvents
import com.ruleup.observability.data.context.ScreenContextHolder
import com.ruleup.observability.domain.model.ScreenKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 화면 진입 추적. 네비게이션이 부르는 단일 진입점이다.
 *
 * 화면이 바뀔 때 해야 할 일이 셋인데 — jank 창 확정, 관측 컨텍스트 갱신, `screen_view` 기록 —
 * **호출을 따로 흩뿌리면 순서를 틀리기 쉽다.** 컨텍스트를 먼저 갱신해야 이후 발생하는 진단·성능
 * 이벤트가 새 화면을 달고 나간다. `ResourceProbe`·`DiagnosticPayload` 에는 화면 필드가 없어서
 * 컨텍스트가 유일한 출처다.
 *
 * 비즈니스 이벤트의 화면 출처도 같은 갱신에서 나온다([ScreenPathHolder]) — 기록기가 그 홀더에서
 * 읽어 가므로, 진입을 기록하기 전에 홀더를 먼저 바꿔야 이벤트에 새 화면이 실린다.
 *
 * 이전 화면을 들고 있다가 `from_screen` 으로 넘긴다 — 유입 경로 분석이 가능해진다.
 * 메인 스레드의 네비게이션 흐름에서만 갱신되고, 읽기는 임의 스레드에서 올 수 있다.
 */
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
            //
            // 순서가 중요하다 — jank 창은 컨텍스트에서 화면을 읽으므로 반드시 setScreen 전에 닫는다.
            // TTI 는 더 이상 여기서 끊지 않는다. 화면의 수명은 컴포지션이 쥐고 있어서
            // (`TtiPage`), 네비게이션이 대신 끊으면 아직 그려지는 중인 화면을 잘라 버린다.
            jankTracker.onScreenChanged()
            contextHolder.setScreen(ScreenKey(path))
            // 기록기가 홀더에서 화면을 읽어 가므로, 이벤트에 새 화면이 실리려면 기록보다 먼저 바꿔야 한다.
            screenPathHolder.setScreen(path)
            current = path
            bizLogger.record(CommonBizEvents.screenView(screen = path, from = from))
        }
    }
