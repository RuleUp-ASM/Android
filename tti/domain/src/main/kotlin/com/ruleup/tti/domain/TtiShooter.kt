package com.ruleup.tti.domain

/**
 * 완성된 측정을 내보내는 곳. **어디로 보낼지는 앱이 정한다**(`:app` 이 바인딩한다).
 *
 * IO 디스패처에서 불린다.
 */
fun interface TtiShooter {
    /** 실패하면 그대로 예외를 던진다 — 기록은 지워지지 않고 다음 기회에 다시 나간다. */
    suspend fun shoot(records: List<TtiRecord>)
}
