package com.ruleup.tti.domain

/** 완성된 측정을 내보내는 곳. */
fun interface TtiShooter {
    /** 실패하면 그대로 예외를 던진다 */
    suspend fun shoot(records: List<TtiRecord>)
}
