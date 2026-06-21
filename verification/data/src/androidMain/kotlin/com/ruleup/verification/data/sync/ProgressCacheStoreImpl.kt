package com.ruleup.verification.data.sync

import com.ruleup.verification.data.db.ProgressCacheDao
import com.ruleup.verification.data.db.ProgressCacheEntity
import com.ruleup.verification.domain.entity.TodayStatus
import com.ruleup.verification.domain.entity.UpdatedChallenge
import com.ruleup.verification.domain.port.ProgressCacheStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * sync 응답 진행률 캐시(Room). 홈 관찰자가 [observe] 로 즉시 반영(명세 §3.3·§6.1).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ProgressCacheStoreImpl(
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
