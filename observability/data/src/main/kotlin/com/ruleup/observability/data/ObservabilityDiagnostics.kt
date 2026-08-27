package com.ruleup.observability.data

import com.ruleup.observability.data.policy.PolicyConfig
import com.ruleup.observability.data.policy.RuntimePolicy
import com.ruleup.observability.data.sink.FirebaseEventMapper
import com.ruleup.observability.data.sink.SinkFailureReporter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 인스펙터가 *"이 로그가 왜 안 찍히지"* 에 답하기 위해 읽는 값들. 게이트에서 죽은 이벤트는
 * `ObsEvent` 가 만들어지기도 전에 사라져 **어디에도 흔적이 없어서**, 설정 스냅샷을 그대로 노출한다.
 */
@Singleton
class ObservabilityDiagnostics
    @Inject
    constructor(
        private val policy: RuntimePolicy,
        private val failureReporter: SinkFailureReporter,
    ) {
        /** 현재 게이트 설정 스냅샷. 오버레이가 채널별 칩을 그리는 데 쓴다. */
        fun config(): PolicyConfig = policy.config()

        /**
         * 비정상 신호 요약. **0 이면 빈 문자열**이다 — 평소 0 이라 항상 띄우면 잡음이지만,
         * 0 이 아닌 순간에는 *"수집이 조용히 망가진 상태"* 를 알리는 유일한 신호다.
         */
        fun anomalySummary(): String =
            buildString {
                val truncated = truncatedCount()
                if (truncated > 0) append("✂$truncated ")
                val failures = sinkFailures().values.sum()
                if (failures > 0) append("⚠$failures")
            }.trim()

        /** Firebase 매핑에서 제약을 넘겨 잘린 누적 횟수. 0 이 아니면 분석 값이 뭉치고 있다는 뜻이다. */
        fun truncatedCount(): Long = FirebaseEventMapper.truncated

        /** `(싱크#예외타입) → 누적 실패 수`. 비어 있지 않으면 수집이 조용히 멈춘 구간이 있다. */
        fun sinkFailures(): Map<String, Long> = failureReporter.snapshot()
    }
