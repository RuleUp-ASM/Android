package com.ruleup.tti.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

/** 측정 기록 저장소. */
@Database(
    entities = [TtiRecordEntity::class, TtiSpanEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class TtiDatabase : RoomDatabase() {
    abstract fun ttiDao(): TtiDao
}
