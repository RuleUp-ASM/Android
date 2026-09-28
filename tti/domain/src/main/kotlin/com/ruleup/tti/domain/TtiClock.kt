package com.ruleup.tti.domain

/** 시각을 읽는 포트. */
interface TtiClock {
    /** 구간 길이를 재는 단조 시각(ms). */
    fun elapsedMillis(): Long

    /** 오래 남은 미완성 기록을 가려내는 데만 쓰는 벽시계 시각(ms). */
    fun wallTimeMillis(): Long
}
