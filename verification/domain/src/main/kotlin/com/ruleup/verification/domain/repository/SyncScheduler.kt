package com.ruleup.verification.domain.repository

/** 백그라운드 sync 스케줄 포트. */
interface SyncScheduler {
    fun cancel()

    /** 30분 주기 sync 를 보장(이미 예약돼 있으면 유지). */
    fun ensureScheduled()

    /** 서버가 내린 간격(`flushIntervalSec`)으로 다음 주기를 동적 재설정한다. */
    fun reschedule(flushIntervalSec: Int)

    /** push(지오펜스 발화 등) 발생 시 expedited OneTimeWork 로 즉시 catch-up flush 를 건다. */
    fun enqueueCatchUp()
}
