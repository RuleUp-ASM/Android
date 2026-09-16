package com.ruleup.android_ruleup.logging

import com.ruleup.logging.domain.BizScreenSource
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 지금 보고 있는 화면 경로를 들고 있는 홀더. `ScreenTracker` 가 네비게이션마다 갱신한다.
 *
 * 추적기 자신이 [BizScreenSource] 를 겸하지 않는 이유는 순환이다 — 추적기는 화면 진입을 기록하려고
 * `BizLogger` 를 받고, 기록기는 화면을 알려고 출처를 받는다. 값만 들고 있는 홀더를 사이에 두면
 * 의존이 한 방향으로 펴진다.
 */
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
