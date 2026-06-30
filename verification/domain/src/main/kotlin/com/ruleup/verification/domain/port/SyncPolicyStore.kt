package com.ruleup.verification.domain.port

import com.ruleup.verification.domain.entity.SyncPolicy

/**
 * 서버 정책 영속 포트(전송 스펙 §0.3). Phase 0/sync 응답으로 받은 [SyncPolicy] 를 보관해
 * 스케줄 cadence·백오프에 쓴다. data 가 DataStore 로 구현한다.
 */
interface SyncPolicyStore {
    suspend fun save(policy: SyncPolicy)
}
