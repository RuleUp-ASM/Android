package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SyncPolicy

/**
 * 서버 정책 영속 포트(전송 스펙 §0.3). Phase 0/sync 응답으로 받은 [SyncPolicy] 중 MVP 가 쓰는
 * flushIntervalSec 만 보관해 sync 주기에 쓴다. data 가 DataStore 로 구현한다.
 */
fun interface SyncPolicyStore {
    suspend fun save(policy: SyncPolicy)
}
