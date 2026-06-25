package com.ruleup.verification.domain.port

/**
 * 백그라운드 sync 스케줄 포트(명세 §3.1). Android 구현이 WorkManager PeriodicWork 로 채운다.
 * 정확 알람(AlarmManager exact)은 쓰지 않는다 — Doze/App Standby 를 견디는 설계.
 */
interface SyncScheduler {
    /** 30분 주기 sync 를 보장(이미 예약돼 있으면 유지). 앱 시작 시 호출. */
    fun ensureScheduled()

    /** 서버 권장 간격(nextSyncAfterSec)으로 다음 주기를 동적 재설정한다. */
    fun reschedule(nextSyncAfterSec: Int)
}
