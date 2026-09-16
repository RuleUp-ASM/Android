package com.ruleup.logging.data.sink

import com.ruleup.logging.domain.BizLog
import com.ruleup.logging.domain.BizLogShooter
import kotlin.coroutines.cancellation.CancellationException

/**
 * 팬아웃 + 자식별 실패 격리. 한 백엔드가 죽어도 나머지로는 계속 나가야 한다 —
 * 두 출구를 병행하는 이유가 그것이다.
 *
 * 첫 실패는 모아뒀다 루프 종료 후 던진다. 기록기가 그것을 받아 삼키고 다음 건으로 넘어가므로
 * 화면은 영향을 받지 않고, 실패 사실은 [BizShooterFailureReporter] 에 남는다.
 */
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
                // "로깅이 실패했다"가 아니라 "JVM 이 더 못 간다" — 삼키면 무관한 지점에서 죽는다.
                if (t is VirtualMachineError) throw t
                failureReporter.onFailure(child::class.simpleName ?: "BizLogShooter", t)
                if (failure == null) failure = t
            }
        }
        failure?.let { throw it }
    }
}
