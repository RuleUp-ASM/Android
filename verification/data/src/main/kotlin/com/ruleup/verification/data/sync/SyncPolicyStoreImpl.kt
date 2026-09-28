package com.ruleup.verification.data.sync

import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.repository.SyncPolicyStore
import javax.inject.Inject

/** 서버 정책 영속. */
class SyncPolicyStoreImpl
    @Inject
    constructor(
        private val settings: VerificationSettingsStore,
    ) : SyncPolicyStore {
        override suspend fun save(policy: SyncPolicy) {
            settings.setFlushIntervalSec(policy.flushIntervalSec.toLong())
        }
    }
