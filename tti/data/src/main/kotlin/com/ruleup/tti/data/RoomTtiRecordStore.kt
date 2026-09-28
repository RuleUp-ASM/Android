package com.ruleup.tti.data

import com.ruleup.tti.data.room.TtiDao
import com.ruleup.tti.data.room.TtiRecordEntity
import com.ruleup.tti.data.room.TtiSpanEntity
import com.ruleup.tti.data.room.toTtiRecord
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiRecordStore
import com.ruleup.tti.domain.TtiTimeline
import javax.inject.Inject

/** Room 으로 받은 [TtiRecordStore]. */
internal class RoomTtiRecordStore
    @Inject
    constructor(
        private val dao: TtiDao,
    ) : TtiRecordStore {
        /** 기록 행과 구간 행을 잇달아 넣는다. */
        override suspend fun openSpan(
            tti: Tti,
            pageName: String,
            createdAt: Long,
            timeline: TtiTimeline,
            startedAt: Long,
        ) {
            dao.insertRecord(TtiRecordEntity(id = tti.id, pageName = pageName, createdAt = createdAt))
            dao.insertSpan(
                TtiSpanEntity(
                    recordId = tti.id,
                    timeline = timeline.name,
                    startedAt = startedAt,
                    endedAt = null,
                ),
            )
        }

        override suspend fun closeSpan(
            tti: Tti,
            timeline: TtiTimeline,
            endedAt: Long,
        ) {
            dao.endSpan(recordId = tti.id, timeline = timeline.name, endedAt = endedAt)
        }

        override suspend fun find(tti: Tti): TtiRecord? = dao.findRecord(tti.id)?.toTtiRecord()

        override suspend fun findAll(): List<TtiRecord> = dao.findAllRecords().map { it.toTtiRecord() }

        override suspend fun delete(ttis: List<Tti>) {
            dao.deleteRecords(ttis.map { it.id })
        }

        override suspend fun deleteCreatedBefore(threshold: Long) {
            dao.deleteCreatedBefore(threshold)
        }
    }
