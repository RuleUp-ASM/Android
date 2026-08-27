package com.ruleup.verification.data.sync

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * sync 를 프로세스 단위 single-flight 로 묶는다 — 주기 work 와 catch-up 은 unique work name 이 달라
 * WorkManager 가 겹침을 막지 못하고, 겹치면 같은 신호가 두 번 나간다. 근거와 CAS 선택은 #355.
 */
@Singleton
class SyncGate
    @Inject
    constructor() {
        private val running = AtomicBoolean(false)

        /** 진입에 성공하면 true. false 면 다른 실행이 드레인 중이라 이번 실행은 들어가면 안 된다. */
        fun tryEnter(): Boolean = running.compareAndSet(false, true)

        /** [tryEnter] 가 true 를 준 실행만 호출한다. 호출자의 finally 에서 반드시 풀어야 한다. */
        fun leave() {
            running.set(false)
        }
    }
