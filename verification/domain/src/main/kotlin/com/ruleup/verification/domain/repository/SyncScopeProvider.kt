package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SignalScope

/** 현재 활성 챌린지 기준 신호 수집 스코프를 제공한다. */
interface SyncScopeProvider {
    suspend fun currentScope(): SignalScope
}
