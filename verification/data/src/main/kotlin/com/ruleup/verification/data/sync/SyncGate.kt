package com.ruleup.verification.data.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** sync 를 프로세스 단위 single-flight 로 묶는다 */
@Singleton
class SyncGate
    @Inject
    constructor() {
        private val running = Mutex()

        suspend fun <T> exclusively(action: suspend () -> T): T = running.withLock { action() }

        /** 진입에 성공하면 true. */
        fun tryEnter(): Boolean = running.tryLock()

        /** [tryEnter] 가 true 를 준 실행만 호출한다. */
        fun leave() {
            running.unlock()
        }
    }
