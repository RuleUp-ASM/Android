package com.ruleup.verification.data.sync

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * sync 실행을 프로세스 단위 single-flight 로 묶는 게이트(#355).
 *
 * [com.ruleup.verification.data.sync.VerificationSyncWorker] 를 깨우는 경로가 둘 이상인데
 * unique work name 이 서로 달라(주기 `verification_sync` · catch-up `verification_sync_catchup`)
 * WorkManager 가 겹침을 막지 못한다. 두 실행이 하나의 버퍼를 나눠 밟으면 뒤 실행의 `tagPending` 이
 * 앞 실행이 들고 있던 행까지 자기 키로 덮어써(#319 로 `WHERE synced = 0` 을 넓힌 결과) 같은 신호가
 * 두 번 나가고 두 번째가 429 를 태운다.
 *
 * 워커는 모두 같은 프로세스에서 돌므로 싱글톤 하나로 실제 직렬화된다.
 *
 * **기다리지 않고 즉시 실패한다** — 겹친 실행은 어차피 같은 버퍼를 보므로 줄 서서 한 번 더 보낼 이유가
 * 없다. 호출자가 재시도를 예약해 다음 기회에 드레인하는 편이 맞다. 그래서 Mutex 가 아니라 CAS 다.
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
