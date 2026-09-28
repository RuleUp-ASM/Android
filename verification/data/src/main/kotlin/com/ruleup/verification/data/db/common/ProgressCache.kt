package com.ruleup.verification.data.db.common

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** sync 응답 updatedChallenges 진행률 캐시. */
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

    /** envelope `activeChallengeIds` 소스 */
    @Query("SELECT challengeId FROM progress_cache")
    suspend fun allChallengeIds(): List<String>
}
