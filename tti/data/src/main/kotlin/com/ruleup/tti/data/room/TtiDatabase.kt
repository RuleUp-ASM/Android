package com.ruleup.tti.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 측정 기록 저장소. `verification` 의 DB 와 따로 둔다 — 계측 데이터는 잃어도 앱 동작에 영향이
 * 없어서 보존 정책이 다르다.
 *
 * **마이그레이션을 쌓지 않고 스키마가 바뀌면 버린다.** 모듈 안에서만 쓰는 테이블이고, 계측을
 * 지키려고 마이그레이션을 관리하는 비용이 얻는 것보다 크다.
 */
@Database(
    entities = [TtiRecordEntity::class, TtiSpanEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class TtiDatabase : RoomDatabase() {
    abstract fun ttiDao(): TtiDao
}
