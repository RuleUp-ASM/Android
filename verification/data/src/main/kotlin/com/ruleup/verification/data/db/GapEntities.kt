package com.ruleup.verification.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.SignalGap

/**
 * 신호 공백 버퍼 (전송 스펙 §0.5 `gaps[]`). 수집기가 공백 사유 발생 즉시 적재하고,
 * sync 가 신호 배치와 같은 멱등 키로 드레인해 envelope 로 보낸다. 신호 버퍼와 동일한
 * tagPending → byBatch → markSynced 흐름을 따른다.
 */
@Entity(tableName = "signal_gap")
data class SignalGapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val signalType: String,
    // GapReason.name
    val reason: String,
    val fromMillis: Long,
    val toMillis: Long,
    val recoverable: Boolean,
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

@Dao
interface SignalGapDao {
    @Insert
    suspend fun insert(entity: SignalGapEntity)

    @Query("UPDATE signal_gap SET collectedAt = :key WHERE synced = 0 AND collectedAt IS NULL")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM signal_gap WHERE collectedAt = :key AND synced = 0")
    suspend fun byBatch(key: String): List<SignalGapEntity>

    @Query("UPDATE signal_gap SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query("DELETE FROM signal_gap WHERE synced = 1 AND occurredAt < :threshold")
    suspend fun purge(threshold: Long)
}

internal fun SignalGapEntity.toDomain(): SignalGap =
    SignalGap(
        signalType = signalType,
        reason = runCatching { GapReason.valueOf(reason) }.getOrDefault(GapReason.PERMISSION_MISSING),
        fromMillis = fromMillis,
        toMillis = toMillis,
        recoverable = recoverable,
    )
