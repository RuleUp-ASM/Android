package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.common.toDomain
import com.ruleup.verification.data.db.common.toEntity
import com.ruleup.verification.data.db.health.HealthSettingsEntity
import com.ruleup.verification.data.db.health.HealthTargetDao
import com.ruleup.verification.domain.entity.HealthTarget
import com.ruleup.verification.domain.repository.HealthTargetStore
import javax.inject.Inject

/**
 * 움직임·수면(HEALTH·SLEEP) 수집 대상 로컬 보관(명세 §3.2·§8). 셋업 플로우가 채우고 sync 스코프가 읽는다.
 */
class HealthTargetStoreImpl
    @Inject
    constructor(
        private val healthTargetDao: HealthTargetDao,
    ) : HealthTargetStore {
        override suspend fun replaceAll(
            targets: Set<HealthTarget>,
            sleepRequested: Boolean,
        ) {
            healthTargetDao.clear()
            if (targets.isNotEmpty()) {
                healthTargetDao.upsertAll(targets.map { it.toEntity() })
            }
            healthTargetDao.setSettings(HealthSettingsEntity(sleepRequested = sleepRequested))
        }

        override suspend fun all(): Set<HealthTarget> = healthTargetDao.all().mapTo(HashSet()) { it.toDomain() }

        override suspend fun sleepRequested(): Boolean = healthTargetDao.sleepRequested() ?: false
    }
