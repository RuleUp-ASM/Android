package com.ruleup.verification.data.db.common

import androidx.room.TypeConverter
import com.ruleup.verification.data.db.usage.UsageEventKind
import com.ruleup.verification.data.db.usage.UsageEventType
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.RecordingMethod

/**
 * 버퍼 enum ↔ TEXT 변환(Room). 저장 형식은 [Enum.name] 그대로라 컬럼은 그대로 `TEXT NOT NULL` 이다 —
 * 스키마가 그대로여야 `version = 8` 을 올리지 않고도 기존 DB 가 열린다.
 *
 * 읽기에 `valueOf` 를 쓰지 않는다. 구버전으로 되돌아가 모르는 값을 만나면 예외가 그 행이 아니라
 * **sync 드레인 전체**를 죽인다. 대신 아래 폴백으로 떨어뜨리되, 중립인 값이 없으므로 매번
 * "틀렸을 때 덜 다치는 쪽"을 골랐다.
 */
class VerificationTypeConverters {
    @TypeConverter
    fun fromUsageEventKind(value: UsageEventKind): String = value.name

    /** 미인식은 SCREEN — APP 으로 접으면 없던 앱 사용 이벤트가 서버 판정에 들어간다. */
    @TypeConverter
    fun toUsageEventKind(value: String): UsageEventKind = value.toEnumOr(UsageEventKind.SCREEN)

    @TypeConverter
    fun fromUsageEventType(value: UsageEventType): String = value.name

    /** 미인식은 STOPPED — 짝 없는 종료는 사용 시간을 만들지 않는다. RESUMED·UNLOCK 은 없던 사용·기상을 만든다. */
    @TypeConverter
    fun toUsageEventType(value: String): UsageEventType = value.toEnumOr(UsageEventType.STOPPED)

    @TypeConverter
    fun fromHealthMetric(value: HealthMetric): String = value.name

    /** 미인식은 STEPS — 목표 수치가 가장 커서 다른 지표의 값이 섞여 들어와도 목표를 통과시키지 않는다. */
    @TypeConverter
    fun toHealthMetric(value: String): HealthMetric = value.toEnumOr(HealthMetric.STEPS)

    @TypeConverter
    fun fromRecordingMethod(value: RecordingMethod): String = value.name

    /** 미인식은 [RecordingMethod.UNKNOWN] — 도메인이 그 자리로 정해 둔 값이다(명세 §8.2 신뢰 게이트). */
    @TypeConverter
    fun toRecordingMethod(value: String): RecordingMethod = value.toEnumOr(RecordingMethod.UNKNOWN)

    @TypeConverter
    fun fromGapReason(value: GapReason): String = value.name

    /** 미인식은 BUFFER_EVICTED — PERMISSION_MISSING 으로 접으면 서버가 멀쩡한 권한을 없는 것으로 읽는다. */
    @TypeConverter
    fun toGapReason(value: String): GapReason = value.toEnumOr(GapReason.BUFFER_EVICTED)
}

private inline fun <reified T : Enum<T>> String.toEnumOr(fallback: T): T = enumValues<T>().find { it.name == this } ?: fallback
