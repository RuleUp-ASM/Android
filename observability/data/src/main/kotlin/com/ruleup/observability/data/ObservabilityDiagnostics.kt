package com.ruleup.observability.data

import com.ruleup.observability.data.policy.PolicyConfig
import com.ruleup.observability.data.policy.RuntimePolicy
import com.ruleup.observability.data.sink.FirebaseEventMapper
import com.ruleup.observability.data.sink.SinkFailureReporter
import javax.inject.Inject
import javax.inject.Singleton

/** 인스펙터가 *"이 로그가 왜 안 찍히지"* 에 답하기 위해 읽는 값들. */
@Singleton
class ObservabilityDiagnostics
    @Inject
    constructor(
        private val policy: RuntimePolicy,
        private val failureReporter: SinkFailureReporter,
    ) {
        /** 현재 게이트 설정 스냅샷. */
        fun config(): PolicyConfig = policy.config()

        /** 비정상 신호 요약. */
        fun anomalySummary(): String =
            buildString {
                val truncated = truncatedCount()
                if (truncated > 0) append("✂$truncated ")
                val failures = sinkFailures().values.sum()
                if (failures > 0) append("⚠$failures")
            }.trim()

        /** Firebase 매핑에서 제약을 넘겨 잘린 누적 횟수. */
        fun truncatedCount(): Long = FirebaseEventMapper.truncated

        /** `(싱크#예외타입) → 누적 실패 수`. */
        fun sinkFailures(): Map<String, Long> = failureReporter.snapshot()
    }
