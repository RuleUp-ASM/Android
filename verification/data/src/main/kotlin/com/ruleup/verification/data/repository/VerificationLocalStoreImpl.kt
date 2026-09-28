package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.common.VerificationDatabase
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.repository.VerificationLocalStore
import javax.inject.Inject

/** 수집 버퍼 전체를 비운다. */
class VerificationLocalStoreImpl
    @Inject
    constructor(
        private val database: VerificationDatabase,
        private val settingsStore: VerificationSettingsStore,
    ) : VerificationLocalStore {
        override suspend fun clearAll() {
            database.clearAllTables()
            settingsStore.clear()
        }
    }
