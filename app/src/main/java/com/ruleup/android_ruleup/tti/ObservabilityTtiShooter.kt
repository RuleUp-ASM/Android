package com.ruleup.android_ruleup.tti

import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.PerformancePayload
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiShooter
import com.ruleup.tti.domain.TtiTimeline
import javax.inject.Inject

/**
 * 완성된 TTI 를 관측 파이프라인으로 흘린다. **어디로 보낼지는 앱이 정한다**는 계약이 여기서 닫힌다 —
 * `:tti` 는 Amplitude 도 Firebase 도 모른다.
 *
 * 완성된 기록만 들어오므로 [TtiRecord.totalTimeMillis] 는 항상 값이 있다. 그래도 null 이면
 * 조용히 건너뛴다 — 계측 때문에 앱이 죽는 일은 없어야 한다.
 */
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

        /**
         * 구간 이름 → 길이. 키를 enum 이름 그대로 쓰는 것은 대시보드에서 코드와 같은 어휘로 찾게
         * 하려는 것이다(매퍼가 소문자로 내린다).
         */
        private fun TtiRecord.spanMillis(): Map<String, Long> =
            TtiTimeline.entries
                .mapNotNull { timeline ->
                    spans[timeline]?.durationMillis?.let { timeline.name to it }
                }.toMap()
    }
