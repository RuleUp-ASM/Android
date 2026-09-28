package com.ruleup.logging.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** [BizLogger] 구현. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class BizLoggerImpl(
    private val shooter: BizLogShooter,
    private val clock: BizLogClock,
    private val screenSource: BizScreenSource,
    private val userSource: BizUserSource,
    private val ioDispatcher: CoroutineDispatcher,
) : BizLogger {
    /** [init] 과 [destroy] 사이에만 살아 있다. */
    @Volatile
    private var scope: CoroutineScope? = null

    /** 전송 하나를 통째로 감싸는 잠금. */
    private val shotMutex = Mutex()

    override fun init() {
        synchronized(this) {
            if (scope != null) return
            scope = CoroutineScope(ioDispatcher.limitedParallelism(1) + SupervisorJob())
        }
    }

    override fun record(event: BizEvent) {
        // 화면·사용자는 일어난 시점의 값이어야 한다
        shot(
            BizLog(
                event = event,
                screen = runCatching { screenSource.currentScreen() }.getOrNull(),
                userId = runCatching { userSource.currentUserId() }.getOrNull(),
                recordedAt = clock.nowMillis(),
            ),
        )
    }

    override fun destroy() {
        val closing = synchronized(this) { scope.also { scope = null } } ?: return
        // 아직 나가지 못한 전송이 끝난 다음에 스코프를 접는다.
        closing
            .launch { shotMutex.withLock { } }
            .invokeOnCompletion { closing.cancel() }
    }

    /** 한 건을 IO 스코프에 얹는다. */
    private fun shot(log: BizLog) {
        // init 전이거나 destroy 뒤면 보낼 곳이 없다.
        val scope = scope ?: return warn("${log.event.name} 기록 버림", "기록기가 열려 있지 않다")
        scope.launch {
            runCatching { shotMutex.withLock { shooter.shoot(log) } }
                .onFailure { warn("${log.event.name} 전송 실패", it.describe()) }
        }
    }

    /** 로그가 실패했다는 사실만 남긴다. */
    private fun warn(
        what: String,
        cause: String,
    ) {
        println("$TAG: $what ($cause)")
    }

    private fun Throwable.describe(): String = "${javaClass.simpleName}: $message"

    private companion object {
        const val TAG = "BIZLOG"
    }
}
