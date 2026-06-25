package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.UsageTargetDao
import com.ruleup.verification.data.db.UsageTargetEntity
import com.ruleup.verification.domain.port.UsageTargetStore
import javax.inject.Inject

/**
 * SCREEN_TIME 대상 패키지 로컬 보관(명세 §3.2). 참여/설정 플로우가 채우고 sync 스코프가 읽는다.
 */
class UsageTargetStoreImpl
    @Inject
    constructor(
        private val usageTargetDao: UsageTargetDao,
    ) : UsageTargetStore {
        override suspend fun replaceAll(packages: Set<String>) {
            usageTargetDao.clear()
            if (packages.isNotEmpty()) {
                usageTargetDao.upsertAll(packages.map { UsageTargetEntity(packageName = it) })
            }
        }

        override suspend fun all(): Set<String> = usageTargetDao.all().toHashSet()
    }
