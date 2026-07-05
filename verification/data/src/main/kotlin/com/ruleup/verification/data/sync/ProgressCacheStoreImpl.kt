package com.ruleup.verification.data.sync

import com.ruleup.verification.data.db.common.ProgressCacheDao
import com.ruleup.verification.data.db.common.ProgressCacheEntity
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.UpdatedChallenge
import com.ruleup.verification.domain.repository.ProgressCacheStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * sync 응답 진행률 캐시(Room). 홈 관찰자가 [observe] 로 즉시 반영(명세 §3.3·§6.1).
 */
class ProgressCacheStoreImpl
    @Inject
    constructor(
        private val progressCacheDao: ProgressCacheDao,
    ) : ProgressCacheStore {
        override suspend fun upsert(updated: List<UpdatedChallenge>) {
            if (updated.isEmpty()) return
            val now = System.currentTimeMillis()
            progressCacheDao.upsertAll(
                updated.map {
                    ProgressCacheEntity(
                        challengeId = it.challengeId,
                        todayStatus = it.todayStatus.name,
                        progressRate = it.progressRate,
                        updatedAt = now,
                    )
                },
            )
        }

        override fun observe(): Flow<List<UpdatedChallenge>> =
            progressCacheDao.observeAll().map { list ->
                list.map {
                    UpdatedChallenge(
                        challengeId = it.challengeId,
                        todayStatus = TodayStatus.fromValue(it.todayStatus),
                        progressRate = it.progressRate,
                    )
                }
            }
    }
