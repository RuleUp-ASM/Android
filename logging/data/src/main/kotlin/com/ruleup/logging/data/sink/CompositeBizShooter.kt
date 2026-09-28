package com.ruleup.logging.data.sink

import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter
import kotlin.coroutines.cancellation.CancellationException

/** 팬아웃 + 자식별 실패 격리. */
internal class CompositeBizShooter(
    private val children: List<BizLogShooter>,
    private val failureReporter: BizShooterFailureReporter,
) : BizLogShooter {
    override suspend fun shoot(log: BizLog) {
        var failure: Throwable? = null
        for (child in children) {
            try {
                child.shoot(log)
            } catch (t: Throwable) {
                // 삼키면 구조적 동시성이 조용히 깨진다.
                if (t is CancellationException) throw t
                // "로깅이 실패했다"가 아니라 "JVM 이 더 못 간다"
                if (t is VirtualMachineError) throw t
                failureReporter.onFailure(child::class.simpleName ?: "BizLogShooter", t)
                if (failure == null) failure = t
            }
        }
        failure?.let { throw it }
    }
}
