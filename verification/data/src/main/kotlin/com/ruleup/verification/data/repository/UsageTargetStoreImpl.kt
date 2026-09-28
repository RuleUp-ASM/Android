package com.ruleup.verification.data.repository

import com.ruleup.verification.data.db.usage.UsageTargetDao
import com.ruleup.verification.data.db.usage.UsageTargetEntity
import com.ruleup.verification.domain.repository.UsageTargetStore
import javax.inject.Inject

/** SCREEN_TIME 대상 패키지 로컬 보관. */
class UsageTargetStoreImpl
    @Inject
    constructor(
        private val usageTargetDao: UsageTargetDao,
    ) : UsageTargetStore {
        override suspend fun replaceAll(targets: Map<String, Set<String>>) {
            usageTargetDao.replaceAll(
                targets.flatMap { (id, packages) ->
                    packages.map { UsageTargetEntity(challengeId = id, packageName = it) }
                },
            )
        }

        override suspend fun replaceFor(
            challengeId: String,
            packages: Set<String>,
        ) {
            // 지우고 넣는 순서다.
            usageTargetDao.clearChallenge(challengeId)
            if (packages.isNotEmpty()) {
                usageTargetDao.upsertAll(
                    packages.map { UsageTargetEntity(challengeId = challengeId, packageName = it) },
                )
            }
        }

        override suspend fun all(): Set<String> = usageTargetDao.all().toHashSet()
    }
