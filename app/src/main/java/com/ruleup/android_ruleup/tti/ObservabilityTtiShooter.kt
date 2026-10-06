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
                // 화면마다 trace 를 나눠야 콘솔 목록에서 화면별 분포가 바로 보인다(#564).
                val trace = FirebasePerformance.getInstance().newTrace(ttiTraceName(record.pageName))
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

/**
 * 화면별 trace 이름. Firebase 는 trace 이름에 앞뒤 공백·선행 `_` 를 막고 100자로 자른다.
 * page_name 속성은 원래 이름 그대로 함께 싣는다.
 */
internal fun ttiTraceName(pageName: String): String {
    val safe = pageName.replace(Regex("[^A-Za-z0-9_]"), "_").trim('_')
    return "$TTI_TRACE_PREFIX${safe.ifEmpty { "unknown" }}".take(TRACE_NAME_MAX)
}

private const val TTI_TRACE_PREFIX = "tti_"
private const val TRACE_NAME_MAX = 100
