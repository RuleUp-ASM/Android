package com.ruleup.verification.data.db.common

import androidx.room.TypeConverter
import com.ruleup.verification.data.db.usage.UsageEventKind
import com.ruleup.verification.data.db.usage.UsageEventType
import com.ruleup.verification.domain.entity.GapReason
import com.ruleup.verification.domain.entity.HealthMetric
import com.ruleup.verification.domain.entity.RecordingMethod

/** 버퍼 enum ↔ TEXT 변환(Room). */
class VerificationTypeConverters {
    @TypeConverter
    fun fromUsageEventKind(value: UsageEventKind): String = value.name

    /** 미인식은 SCREEN */
    @TypeConverter
    fun toUsageEventKind(value: String): UsageEventKind = value.toEnumOr(UsageEventKind.SCREEN)

    @TypeConverter
    fun fromUsageEventType(value: UsageEventType): String = value.name

    /** 미인식은 STOPPED */
    @TypeConverter
    fun toUsageEventType(value: String): UsageEventType = value.toEnumOr(UsageEventType.STOPPED)

    @TypeConverter
    fun fromHealthMetric(value: HealthMetric): String = value.name

    /** 미인식은 STEPS */
    @TypeConverter
    fun toHealthMetric(value: String): HealthMetric = value.toEnumOr(HealthMetric.STEPS)

    @TypeConverter
    fun fromRecordingMethod(value: RecordingMethod): String = value.name

    /** 미인식은 [RecordingMethod.UNKNOWN] */
    @TypeConverter
    fun toRecordingMethod(value: String): RecordingMethod = value.toEnumOr(RecordingMethod.UNKNOWN)

    @TypeConverter
    fun fromGapReason(value: GapReason): String = value.name

    /** 미인식은 BUFFER_EVICTED */
    @TypeConverter
    fun toGapReason(value: String): GapReason = value.toEnumOr(GapReason.BUFFER_EVICTED)
}

private inline fun <reified T : Enum<T>> String.toEnumOr(fallback: T): T = enumValues<T>().find { it.name == this } ?: fallback
