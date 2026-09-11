package com.ruleup.android_ruleup.observability

import com.ruleup.observability.data.context.ScreenContextHolder
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.BusinessPayload
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.Attributes
import com.ruleup.observability.domain.model.Referrer
import com.ruleup.observability.domain.model.ScreenKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 화면 진입 추적. 네비게이션이 부르는 단일 진입점이다.
 *
 * 화면이 바뀔 때 해야 할 일이 둘인데 — 관측 컨텍스트 갱신과 `ScreenView` 기록 — **호출을 따로
 * 흩뿌리면 순서를 틀리기 쉽다.** 컨텍스트를 먼저 갱신해야 이후 발생하는 진단·성능 이벤트가
 * 새 화면을 달고 나간다. `ResourceProbe`·`DiagnosticPayload` 에는 화면 필드가 없어서
 * 컨텍스트가 유일한 출처다.
 *
 * 이전 화면을 들고 있다가 [Referrer] 로 넘긴다 — 유입 경로 분석이 가능해진다.
 * 메인 스레드의 네비게이션 흐름에서만 호출된다.
 */
@Singleton
class ScreenTracker
    @Inject
    constructor(
        private val observability: Observability,
        private val contextHolder: ScreenContextHolder,
        private val jankTracker: JankTracker,
    ) {
        private var current: ScreenKey? = null

        fun onScreenEntered(path: String) {
            val screen = ScreenKey(path)
            val from = current
            // 진행 중이던 jank 창을 여기서 확정한다.
            //
            // 순서가 중요하다 — jank 창은 컨텍스트에서 화면을 읽으므로 반드시 setScreen 전에 닫는다.
            // TTI 는 더 이상 여기서 끊지 않는다. 화면의 수명은 컴포지션이 쥐고 있어서
            // (`TtiPage`), 네비게이션이 대신 끊으면 아직 그려지는 중인 화면을 잘라 버린다.
            jankTracker.onScreenChanged()
            contextHolder.setScreen(screen)
            current = screen
            observability.log(Channel.BUSINESS) {
                BusinessPayload.ScreenView(
                    screen = screen,
                    referrer = from?.let { Referrer(fromScreen = it, element = null, elementState = Attributes.EMPTY) },
                )
            }
        }
    }
