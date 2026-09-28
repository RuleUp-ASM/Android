package com.ruleup.android_ruleup.logging

import com.ruleup.logging.domain.BizScreenSource
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/** 지금 보고 있는 화면 경로를 들고 있는 홀더. */
@Singleton
class ScreenPathHolder
    @Inject
    constructor() : BizScreenSource {
        private val path = AtomicReference<String?>(null)

        override fun currentScreen(): String? = path.get()

        fun setScreen(screen: String?) {
            path.set(screen)
        }
    }
