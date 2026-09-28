package com.ruleup.tti.domain

import kotlinx.coroutines.CoroutineDispatcher

/** 화면별 TTI 기록기. */
interface TtiRecorder {
    /** 기록을 시작한다. */
    fun init()

    /** [timeline] 구간을 연다. */
    fun startRecord(
        timeline: TtiTimeline,
        tti: Tti,
        pageName: String,
    )

    /** [timeline] 구간을 닫는다. */
    fun endRecord(
        timeline: TtiTimeline,
        tti: Tti,
        pageName: String,
    )

    /** 한 건을 쏜다. */
    fun shot(
        tti: Tti,
        pageName: String,
    )

    /** 기록을 닫는다. */
    fun destroy()
}

/** 기본 구현을 만든다. */
fun createTtiRecorder(
    store: TtiRecordStore,
    shooter: TtiShooter,
    clock: TtiClock,
    ioDispatcher: CoroutineDispatcher,
): TtiRecorder = TtiRecorderImpl(store, shooter, clock, ioDispatcher)
