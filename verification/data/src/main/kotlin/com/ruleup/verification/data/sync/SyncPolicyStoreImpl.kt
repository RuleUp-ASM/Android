package com.ruleup.verification.data.sync

import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.port.SyncPolicyStore
import javax.inject.Inject

/**
 * 서버 정책 영속(전송 스펙 §0.3). MVP 는 flushIntervalSec 를 DataStore 에 보관해 스케줄 cadence 에 쓴다.
 * cadence(pollSec)·backoff 는 향후 수집/재시도에 점진 적용(현재는 미보관).
 */
class SyncPolicyStoreImpl
    @Inject
    constructor(
        private val settings: VerificationSettingsStore,
    ) : SyncPolicyStore {
        override suspend fun save(policy: SyncPolicy) {
            settings.setFlushIntervalSec(policy.flushIntervalSec.toLong())
        }
    }
