package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SignalScope

/**
 * 현재 활성 챌린지 기준 신호 수집 스코프를 제공한다(명세 §3.2 스코핑).
 * Android 구현이 등록된 지오펜스 requestId·대상 패키지·움직임/수면 대상을 모아 채운다.
 */
interface SyncScopeProvider {
    suspend fun currentScope(): SignalScope
}
