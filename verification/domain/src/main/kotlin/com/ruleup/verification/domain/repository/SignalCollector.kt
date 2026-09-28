package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SignalScope

/** OS 신호 수집 포트. */
interface SignalCollector {
    /** 활성 챌린지 스코프로 최신 OS 신호를 수집해 로컬 버퍼([SignalRepository])에 적재한다. */
    suspend fun capture(scope: SignalScope)
}
