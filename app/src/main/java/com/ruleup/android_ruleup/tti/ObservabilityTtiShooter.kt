package com.ruleup.android_ruleup.tti

import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiShooter
import com.ruleup.tti.domain.TtiTimeline
import javax.inject.Inject

/** 완성된 TTI 를 관측 파이프라인으로 흘린다. */
class ObservabilityTtiShooter
    @Inject
    constructor(
        private val observability: Observability,
    ) : TtiShooter {
        override suspend fun shoot(records: List<TtiRecord>) {
            records.forEach { record ->
                val total = record.totalTimeMillis ?: return@forEach
                observability.log(Channel.PERFORMANCE) {
                    PerformancePayload.Tti(
                        pageName = record.pageName,
                        totalMillis = total,
                        spans = record.spanMillis(),
                    )
                }
            }
        }

        /** 구간 이름 → 길이. */
        private fun TtiRecord.spanMillis(): Map<String, Long> =
            TtiTimeline.entries
                .mapNotNull { timeline ->
                    spans[timeline]?.durationMillis?.let { timeline.name to it }
                }.toMap()
    }
