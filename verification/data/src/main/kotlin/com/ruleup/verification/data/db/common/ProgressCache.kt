package com.ruleup.verification.data.db.common

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * sync 응답 updatedChallenges 진행률 캐시(명세 §3.3). 홈이 즉시 반영하도록 비정규화 보관.
 */
@Entity(tableName = "progress_cache")
data class ProgressCacheEntity(
    @PrimaryKey
    val challengeId: String,
    val todayStatus: String,
    val progressRate: Double,
    val updatedAt: Long,
)

@Dao
interface ProgressCacheDao {
    @Upsert
    suspend fun upsertAll(items: List<ProgressCacheEntity>)

    @Query("SELECT * FROM progress_cache ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProgressCacheEntity>>

    /** envelope `activeChallengeIds` 소스(전송 스펙 §0.1) — 클라가 추적 중인 챌린지 id. */
    @Query("SELECT challengeId FROM progress_cache")
    suspend fun allChallengeIds(): List<String>
}
