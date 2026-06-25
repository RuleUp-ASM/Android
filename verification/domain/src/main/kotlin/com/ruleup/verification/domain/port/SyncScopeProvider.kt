package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.SignalScope

/**
 * 현재 활성 챌린지 기준 신호 수집 스코프를 제공한다(명세 §3.2 스코핑).
 * Android 구현이 활성 지오펜스 requestId(Phase 1)·대상 패키지(Phase 3)를 모아 채운다.
 */
interface SyncScopeProvider {
    suspend fun currentScope(): SignalScope
}
