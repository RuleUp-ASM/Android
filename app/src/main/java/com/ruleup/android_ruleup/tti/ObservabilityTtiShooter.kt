package com.ruleup.android_ruleup.tti

import com.google.firebase.perf.FirebasePerformance
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
                val spans = record.spanMillis()
                // 저장된 TTI 는 total_millis 로 본다. Duration 은 이 전송 구간의 시간이다(#518).
                val trace = FirebasePerformance.getInstance().newTrace("tti_shot")
                trace.start()
                try {
                    trace.putAttribute("page_name", record.pageName.take(100))
                    trace.putMetric("total_millis", total)
                    spans.forEach { (name, millis) ->
                        trace.putMetric("${name.lowercase()}_millis", millis)
                    }
                } finally {
                    trace.stop()
                }
                observability.log(Channel.PERFORMANCE) {
                    PerformancePayload.Tti(
                        pageName = record.pageName,
                        totalMillis = total,
                        spans = spans,
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
