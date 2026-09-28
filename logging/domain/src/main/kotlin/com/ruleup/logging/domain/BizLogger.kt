package com.ruleup.logging.domain

import kotlinx.coroutines.CoroutineDispatcher

/** 비즈니스 이벤트 기록기. */
interface BizLogger {
    /** 기록을 시작한다. */
    fun init()

    /** 이벤트 한 건을 남긴다. */
    fun record(event: BizEvent)

    /** 기록을 닫는다. */
    fun destroy()
}

/** 기본 구현을 만든다. */
fun createBizLogger(
    shooter: BizLogShooter,
    clock: BizLogClock,
    screenSource: BizScreenSource,
    userSource: BizUserSource,
    ioDispatcher: CoroutineDispatcher,
): BizLogger = BizLoggerImpl(shooter, clock, screenSource, userSource, ioDispatcher)
