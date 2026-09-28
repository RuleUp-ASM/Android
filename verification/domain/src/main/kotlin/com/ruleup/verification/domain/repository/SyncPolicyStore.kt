package com.ruleup.verification.domain.repository

import com.ruleup.verification.domain.entity.SyncPolicy

/** 서버 정책 영속 포트. */
fun interface SyncPolicyStore {
    suspend fun save(policy: SyncPolicy)
}
