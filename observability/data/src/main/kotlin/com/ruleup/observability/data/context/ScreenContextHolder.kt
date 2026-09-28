package com.ruleup.observability.data.context

import com.ruleup.observability.domain.model.ObsContext
import com.ruleup.observability.domain.model.ScreenKey
import com.ruleup.observability.domain.port.ContextProvider
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** 현재 화면을 들고 있는 가변 홀더. */
@Singleton
class ScreenContextHolder
    @Inject
    constructor() : ContextProvider {
        private val snapshot = AtomicReference(ObsContext(currentScreen = null))

        override fun current(): ObsContext = snapshot.get()

        /** 현재 화면을 갱신한다. */
        fun setScreen(screen: ScreenKey?) {
            snapshot.set(ObsContext(currentScreen = screen))
        }
    }
