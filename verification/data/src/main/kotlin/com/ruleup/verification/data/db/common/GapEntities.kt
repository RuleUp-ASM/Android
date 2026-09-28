package com.ruleup.verification.data.db.common

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.SignalGap

/** 신호 공백 버퍼. */
@Entity(tableName = "signal_gap")
data class SignalGapEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val signalType: String,
    val reason: GapReason,
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

    @Query("UPDATE signal_gap SET collectedAt = :key WHERE synced = 0")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM signal_gap WHERE collectedAt = :key AND synced = 0")
    suspend fun byBatch(key: String): List<SignalGapEntity>

    @Query("UPDATE signal_gap SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    @Query(
        "DELETE FROM signal_gap WHERE occurredAt < :threshold OR id NOT IN (SELECT id FROM signal_gap ORDER BY occurredAt DESC, id DESC LIMIT 1000)",
    )
    suspend fun purge(threshold: Long)
}

internal fun SignalGapEntity.toDomain(): SignalGap =
    SignalGap(
        signalType = signalType,
        reason = reason,
        fromMillis = fromMillis,
        toMillis = toMillis,
        recoverable = recoverable,
    )
