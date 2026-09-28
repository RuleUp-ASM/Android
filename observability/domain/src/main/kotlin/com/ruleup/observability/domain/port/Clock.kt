package com.ruleup.observability.domain.port

/** 이벤트 타임스탬프 발급기. */
interface Clock {
    /** 벽시계(Unix epoch, ms). */
    fun epochMillis(): Long

    /** 단조 증가 시계(ns). */
    fun monotonicNanos(): Long
}
