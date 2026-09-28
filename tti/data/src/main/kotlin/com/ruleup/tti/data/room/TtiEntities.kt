package com.ruleup.tti.data.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiSpan
import com.ruleup.tti.domain.TtiTimeline

/** 측정 한 건(= 화면 인스턴스 하나). */
@Entity(tableName = "tti_record")
internal data class TtiRecordEntity(
    @PrimaryKey val id: String,
    val pageName: String,
    val createdAt: Long,
)

/** 그 측정의 구간 하나. */
@Entity(
    tableName = "tti_span",
    primaryKeys = ["recordId", "timeline"],
    foreignKeys = [
        ForeignKey(
            entity = TtiRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recordId")],
)
internal data class TtiSpanEntity(
    val recordId: String,
    /** [TtiTimeline] 의 이름. */
    val timeline: String,
    val startedAt: Long,
    val endedAt: Long?,
)

/** 기록 + 그 구간들. */
internal data class TtiRecordWithSpans(
    @Embedded val record: TtiRecordEntity,
    @Relation(parentColumn = "id", entityColumn = "recordId")
    val spans: List<TtiSpanEntity>,
)

/** 저장 모양 → 도메인 모양. */
internal fun TtiRecordWithSpans.toTtiRecord(): TtiRecord =
    TtiRecord(
        tti = Tti(record.id),
        pageName = record.pageName,
        createdAt = record.createdAt,
        spans =
            spans
                .mapNotNull { span ->
                    val timeline = TtiTimeline.entries.firstOrNull { it.name == span.timeline } ?: return@mapNotNull null
                    timeline to TtiSpan(startedAt = span.startedAt, endedAt = span.endedAt)
                }.toMap(),
    )
