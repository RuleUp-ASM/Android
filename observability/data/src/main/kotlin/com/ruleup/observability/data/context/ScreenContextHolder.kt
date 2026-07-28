package com.ruleup.observability.data.context

import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.port.ContextProvider
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 화면을 들고 있는 가변 홀더. 읽기 면은 [ContextProvider], 쓰기는 [setScreen] 이다.
 *
 * `:app` 의 네비게이션 콜백이 [setScreen] 을 부른다. 쓰기 계약을 도메인 포트로 두지 않은 이유는
 * 호출자가 `:app` 하나뿐이고, `:app` 은 이 모듈을 직접 볼 수 있기 때문이다.
 *
 * **불변 [ObsContext] 를 통째로 교체한다.** 필드가 늘었을 때 하나씩 갱신하면
 * "화면은 바뀌었는데 다른 필드는 이전 값"인 찢어진 스냅샷이 읽힌다.
 */
@Singleton
class ScreenContextHolder
    @Inject
    constructor() : ContextProvider {
        private val snapshot = AtomicReference(ObsContext(currentScreen = null))

        override fun current(): ObsContext = snapshot.get()

        /**
         * 현재 화면을 갱신한다. 화면을 특정할 수 없으면 null.
         *
         * 필드가 하나뿐이라 단순 교체로 충분하다. 컨텍스트에 필드가 추가되면
         * 기존 값을 보존해야 하므로 `updateAndGet` 으로 바꾼다.
         */
        fun setScreen(screen: ScreenKey?) {
            snapshot.set(ObsContext(currentScreen = screen))
        }
    }
