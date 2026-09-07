package com.ruleup.observability.data.context

import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.port.ContextProvider
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 화면을 들고 있는 가변 홀더. 쓰기 계약을 도메인 포트로 두지 않은 건 호출자가 `:app` 의
 * 네비게이션 콜백 하나뿐이라서다.
 *
 * **불변 [ObsContext] 를 통째로 교체한다** — 필드가 늘었을 때 하나씩 갱신하면 "화면은 바뀌었는데
 * 다른 필드는 이전 값"인 찢어진 스냅샷이 읽힌다.
 */
@Singleton
class ScreenContextHolder
    @Inject
    constructor() : ContextProvider {
        private val snapshot = AtomicReference(ObsContext(currentScreen = null))

        override fun current(): ObsContext = snapshot.get()

        /**
         * 현재 화면을 갱신한다. 화면을 특정할 수 없으면 null.
         * 컨텍스트에 필드가 추가되면 기존 값을 보존해야 하므로 `updateAndGet` 으로 바꾼다.
         */
        fun setScreen(screen: ScreenKey?) {
            snapshot.set(ObsContext(currentScreen = screen))
        }
    }
