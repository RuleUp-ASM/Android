package com.ruleup.tti.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** [TtiRecorder] 구현. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TtiRecorderImpl(
    private val store: TtiRecordStore,
    private val shooter: TtiShooter,
    private val clock: TtiClock,
    private val ioDispatcher: CoroutineDispatcher,
) : TtiRecorder {
    /** [init] 과 [destroy] 사이에만 살아 있다. */
    @Volatile
    private var scope: CoroutineScope? = null

    /** 기록 작업 하나를 통째로 감싸는 잠금. */
    private val opMutex = Mutex()

    override fun init() {
        synchronized(this) {
            if (scope != null) return
            scope = CoroutineScope(ioDispatcher.limitedParallelism(1) + SupervisorJob())
        }
        // 지난 실행이 쏘지 못하고 죽은 것을 먼저 흘려보낸다.
        record("init") {
            shootCompleted()
            store.deleteCreatedBefore(clock.wallTimeMillis() - STALE_THRESHOLD_MILLIS)
        }
    }

    override fun startRecord(
        timeline: TtiTimeline,
        tti: Tti,
        pageName: String,
    ) {
        // 시각은 호출한 스레드에서 지금 찍는다.
        val startedAt = clock.elapsedMillis()
        val createdAt = clock.wallTimeMillis()
        record("start $timeline of $pageName") {
            store.openSpan(
                tti = tti,
                pageName = pageName,
                createdAt = createdAt,
                timeline = timeline,
                startedAt = startedAt,
            )
        }
    }

    override fun endRecord(
        timeline: TtiTimeline,
        tti: Tti,
        pageName: String,
    ) {
        val endedAt = clock.elapsedMillis()
        record("end $timeline of $pageName") {
            store.closeSpan(tti = tti, timeline = timeline, endedAt = endedAt)
        }
    }

    override fun shot(
        tti: Tti,
        pageName: String,
    ) {
        record("shot $pageName") {
            // 앞선 쏘기가 이미 보내고 지웠으면 여기서는 없는 기록이 된다
            val record = store.find(tti) ?: return@record
            // 아직 안 끝난 구간이 있으면 그대로 둔다.
            if (!record.isComplete) return@record
            shootAndDelete(listOf(record))
        }
    }

    override fun destroy() {
        val closing = synchronized(this) { scope.also { scope = null } } ?: return
        // 마지막 쏘기가 끝난 다음에 스코프를 접는다.
        closing
            .launch {
                runCatching { opMutex.withLock { shootCompleted() } }
                    .onFailure { warn("destroy 중 shot 실패", it) }
            }.invokeOnCompletion { closing.cancel() }
    }

    /** 저장소에 남은 완성 기록을 한 번에 쏜다. */
    private suspend fun shootCompleted() {
        val completed = store.findAll().filter { it.isComplete }
        if (completed.isEmpty()) return
        shootAndDelete(completed)
    }

    /** 쏘고 나서 지운다. */
    private suspend fun shootAndDelete(records: List<TtiRecord>) {
        shooter.shoot(records)
        store.delete(records.map { it.tti })
    }

    /** 기록 작업 하나를 IO 스코프에 얹는다. */
    private fun record(
        what: String,
        block: suspend () -> Unit,
    ) {
        val scope = scope ?: return
        scope.launch {
            runCatching { opMutex.withLock { block() } }
                .onFailure { warn("$what 기록 실패", it) }
        }
    }

    /** 계측이 실패했다는 사실만 남긴다. */
    private fun warn(
        what: String,
        cause: Throwable,
    ) {
        println("$TAG: $what (${cause.javaClass.simpleName}: ${cause.message})")
    }

    private companion object {
        const val TAG = "TTI"

        /** 이 시간이 지나도 완성되지 않은 기록은 버린다(뒤로 나가 버린 화면 등). */
        const val STALE_THRESHOLD_MILLIS = 24 * 60 * 60 * 1000L
    }
}
