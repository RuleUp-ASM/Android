package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.common.VerificationDatabase
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.repository.VerificationLocalStore
import javax.inject.Inject

/**
 * 수집 버퍼 전체를 비운다.
 *
 * 테이블을 골라 지우지 않고 [VerificationDatabase.clearAllTables] 로 통째로 비운다 — 이 DB 에는
 * 계정과 무관한 행이 하나도 없고, 골라 지우면 **나중에 테이블이 늘 때 조용히 빠진다.**
 */
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
