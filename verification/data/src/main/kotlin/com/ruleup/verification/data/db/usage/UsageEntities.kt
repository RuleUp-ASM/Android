package com.ruleup.verification.data.db.usage

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.ruleup.verification.domain.entity.AppEventType

/** usage_event 행 구분: 대상 앱 전후면(APP) vs 화면/잠금해제(SCREEN). */
enum class UsageEventKind {
    APP,
    SCREEN,
}

/** usage_event.eventType. */
enum class UsageEventType(
    val appEventType: AppEventType?,
) {
    RESUMED(AppEventType.RESUMED),
    PAUSED(AppEventType.PAUSED),
    STOPPED(AppEventType.STOPPED),
    UNLOCK(null),
    SCREEN_ON(null),
}

/** 스크린타임/WAKE 버퍼. */
@Entity(tableName = "usage_event")
data class UsageEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: UsageEventKind,
    // APP 일 때만 채움(SCREEN 은 "")
    val packageName: String,
    val eventType: UsageEventType,
    val occurredAt: Long,
    val synced: Boolean = false,
    val collectedAt: String? = null,
)

/** SCREEN_TIME 대상 패키지(스코프 소스). */
@Entity(tableName = "usage_target", primaryKeys = ["challengeId", "packageName"])
data class UsageTargetEntity(
    val challengeId: String,
    val packageName: String,
)

/** queryEvents 증분 수집용 커서(직전 조회 시각). */
@Entity(tableName = "usage_cursor")
data class UsageCursorEntity(
    @PrimaryKey
    val id: Int = 0,
    val lastQueriedAt: Long,
)

@Dao
interface UsageEventDao {
    @Insert
    suspend fun insertAll(items: List<UsageEventEntity>)

    @Query("UPDATE usage_event SET collectedAt = :key WHERE synced = 0")
    suspend fun tagPending(key: String)

    @Query("SELECT * FROM usage_event WHERE collectedAt = :key AND synced = 0 ORDER BY occurredAt ASC")
    suspend fun byBatch(key: String): List<UsageEventEntity>

    @Query("UPDATE usage_event SET synced = 1 WHERE collectedAt = :key")
    suspend fun markSynced(key: String)

    /** 당일 첫 화면 이벤트 시각. */
    @Query(
        "SELECT MIN(occurredAt) FROM usage_event " +
            "WHERE kind = 'SCREEN' AND eventType = :eventType AND occurredAt >= :since",
    )
    suspend fun firstScreenEventAt(
        eventType: UsageEventType,
        since: Long,
    ): Long?

    @Query(
        "SELECT MIN(occurredAt) FROM usage_event WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM usage_event ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun oldestEvicted(threshold: Long): Long?

    @Query(
        "SELECT MAX(occurredAt) FROM usage_event WHERE synced = 0 AND (occurredAt < :threshold OR id NOT IN (SELECT id FROM usage_event ORDER BY occurredAt DESC, id DESC LIMIT 10000))",
    )
    suspend fun newestEvicted(threshold: Long): Long?

    @Query(
        "DELETE FROM usage_event WHERE occurredAt < :threshold OR id NOT IN (SELECT id FROM usage_event ORDER BY occurredAt DESC, id DESC LIMIT 10000)",
    )
    suspend fun purge(threshold: Long)
}

@Dao
interface UsageTargetDao {
    @androidx.room.Transaction
    suspend fun replaceAll(items: List<UsageTargetEntity>) {
        clear()
        upsertAll(items)
    }

    /** 수집 스코프는 방을 가리지 않는다 */
    @Query("SELECT DISTINCT packageName FROM usage_target")
    suspend fun all(): List<String>

    @Upsert
    suspend fun upsertAll(items: List<UsageTargetEntity>)

    @Query("DELETE FROM usage_target WHERE challengeId = :challengeId")
    suspend fun clearChallenge(challengeId: String)

    @Query("DELETE FROM usage_target")
    suspend fun clear()
}

@Dao
interface UsageCursorDao {
    @Query("SELECT * FROM usage_cursor WHERE id = 0")
    suspend fun get(): UsageCursorEntity?

    @Upsert
    suspend fun set(cursor: UsageCursorEntity)
}
