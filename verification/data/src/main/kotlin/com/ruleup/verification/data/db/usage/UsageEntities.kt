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

/**
 * usage_event.eventType. 한 컬럼에 두 계열이 섞이므로 [UsageEventKind] 별 값 집합의 합집합이다.
 * [appEventType] 이 있는 값만 APP 행에 오고, 없는 값(UNLOCK·SCREEN_ON)은 SCREEN 행에만 온다.
 */
enum class UsageEventType(
    val appEventType: AppEventType?,
) {
    RESUMED(AppEventType.RESUMED),
    PAUSED(AppEventType.PAUSED),
    STOPPED(AppEventType.STOPPED),
    UNLOCK(null),
    SCREEN_ON(null),
}

/**
 * 스크린타임/WAKE 버퍼(명세 §2.2). 시스템이 며칠 내 정리하므로 매 sync 마다 커서~now 를 누적 적재한다.
 * RESUMED/PAUSED 시퀀스를 그대로 보존(누적 foregroundSec 단일값 금지).
 */
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
@Entity(tableName = "usage_target")
data class UsageTargetEntity(
    @PrimaryKey
    val packageName: String,
)

/** queryEvents 증분 수집용 커서(직전 조회 시각). 단일 행(id=0). */
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

    /**
     * 당일 첫 화면 이벤트 시각(전송 스펙 §4 WAKE). **미전송분이 아니라 [since] 이후 전부**를 본다 —
     * 첫 잠금해제는 하루 한 번뿐이라, 앞선 배치로 이미 나갔으면 이후 sync 에서 값이 사라진다(서버는 멱등).
     */
    @Query(
        "SELECT MIN(occurredAt) FROM usage_event " +
            "WHERE kind = 'SCREEN' AND eventType = :eventType AND occurredAt >= :since",
    )
    suspend fun firstScreenEventAt(
        eventType: UsageEventType,
        since: Long,
    ): Long?

    @Query("DELETE FROM usage_event WHERE synced = 1 AND occurredAt < :threshold")
    suspend fun purge(threshold: Long)
}

@Dao
interface UsageTargetDao {
    @Query("SELECT packageName FROM usage_target")
    suspend fun all(): List<String>

    @Upsert
    suspend fun upsertAll(items: List<UsageTargetEntity>)

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
