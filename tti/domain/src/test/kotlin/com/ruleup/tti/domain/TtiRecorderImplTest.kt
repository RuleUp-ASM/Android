package com.ruleup.tti.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 기록기의 계약. **측정이 조용히 사라지거나 같은 건이 두 번 나가는 것**이 여기서 막힌다.
 *
 * 계측이 실패해도 앱이 죽지 않아야 하므로, 저장소가 던지는 경우도 함께 본다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TtiRecorderImplTest {
    @Test
    fun `네 구간이 닫히면 쏘고 지운다`() =
        runTest {
            val store = FakeStore()
            val shooter = RecordingShooter()
            val recorder = recorder(store, shooter)
            recorder.init()

            recordAll(recorder)
            advanceUntilIdle()

            assertEquals(1, shooter.shot.size)
            assertEquals(
                "challenge_detail",
                shooter.shot
                    .single()
                    .single()
                    .pageName,
            )
            // 쏜 건은 저장소에서 지운다 — 남겨 두면 다음 init 이 같은 건을 또 보낸다.
            assertTrue(store.records.isEmpty())
        }

    @Test
    fun `미완성이면 쏘지 않고 저장소에 남긴다`() =
        runTest {
            val store = FakeStore()
            val shooter = RecordingShooter()
            val recorder = recorder(store, shooter)
            recorder.init()

            val tti = Tti("a")
            recorder.startRecord(TtiTimeline.VIEW_CREATE, tti, PAGE)
            recorder.endRecord(TtiTimeline.VIEW_CREATE, tti, PAGE)
            recorder.shot(tti, PAGE)
            advanceUntilIdle()

            assertTrue(shooter.shot.isEmpty())
            assertEquals(1, store.records.size)
        }

    @Test
    fun `같은 구간의 두 번째 start 는 처음 시각을 지킨다`() =
        runTest {
            // 컴포지션은 언제든 다시 실행된다. 두 번째 start 가 기준을 덮으면 구간이 짧게 잡힌다.
            val store = FakeStore()
            val clock = FakeClock()
            val recorder = recorder(store, RecordingShooter(), clock)
            recorder.init()

            val tti = Tti("a")
            recorder.startRecord(TtiTimeline.VIEW_CREATE, tti, PAGE)
            clock.elapsed = 100
            recorder.startRecord(TtiTimeline.VIEW_CREATE, tti, PAGE)
            advanceUntilIdle()

            assertEquals(
                0,
                store.records
                    .getValue("a")
                    .spans
                    .getValue(TtiTimeline.VIEW_CREATE)
                    .startedAt,
            )
        }

    @Test
    fun `이미 닫힌 구간의 end 는 무시된다`() =
        runTest {
            val store = FakeStore()
            val clock = FakeClock()
            val recorder = recorder(store, RecordingShooter(), clock)
            recorder.init()

            val tti = Tti("a")
            recorder.startRecord(TtiTimeline.BACKEND, tti, PAGE)
            clock.elapsed = 50
            recorder.endRecord(TtiTimeline.BACKEND, tti, PAGE)
            clock.elapsed = 900
            recorder.endRecord(TtiTimeline.BACKEND, tti, PAGE)
            advanceUntilIdle()

            assertEquals(
                50,
                store.records
                    .getValue("a")
                    .spans
                    .getValue(TtiTimeline.BACKEND)
                    .endedAt,
            )
        }

    @Test
    fun `init 은 지난 실행이 남긴 완성 기록을 쏜다`() =
        runTest {
            // 안드로이드는 프로세스 종료를 알려주지 않는다. 못 쏘고 죽은 기록을 실제로 회수하는
            // 곳은 대부분 여기다.
            val store = FakeStore()
            store.records["old"] = completedRecord("old")
            val shooter = RecordingShooter()

            recorder(store, shooter).init()
            advanceUntilIdle()

            assertEquals(listOf("old"), shooter.shot.single().map { it.tti.id })
            assertTrue(store.records.isEmpty())
        }

    @Test
    fun `쏘기가 실패하면 기록을 지우지 않는다`() =
        runTest {
            // 지워 버리면 그 측정은 영영 사라진다. 남겨 두면 다음 기회에 다시 나간다.
            val store = FakeStore()
            store.records["old"] = completedRecord("old")
            val failing = TtiShooter { error("전송 실패") }

            recorder(store, failing).init()
            advanceUntilIdle()

            assertEquals(1, store.records.size)
        }

    @Test
    fun `init 전에 들어온 기록은 조용히 버린다`() =
        runTest {
            // 계측이 없다고 화면이 멈출 이유는 없다.
            val store = FakeStore()
            val recorder = recorder(store, RecordingShooter())

            recorder.startRecord(TtiTimeline.VIEW_CREATE, Tti("a"), PAGE)
            advanceUntilIdle()

            assertTrue(store.records.isEmpty())
        }

    @Test
    fun `저장소가 던져도 기록기는 계속 돈다`() =
        runTest {
            val store = ThrowingOnceStore()
            val shooter = RecordingShooter()
            val recorder = recorder(store, shooter)
            recorder.init()

            recorder.startRecord(TtiTimeline.VIEW_CREATE, Tti("a"), PAGE)
            advanceUntilIdle()
            recordAll(recorder, Tti("b"))
            advanceUntilIdle()

            assertEquals(1, shooter.shot.size)
        }

    private fun recordAll(
        recorder: TtiRecorder,
        tti: Tti = Tti("a"),
    ) {
        TtiTimeline.entries.forEach {
            recorder.startRecord(it, tti, PAGE)
            recorder.endRecord(it, tti, PAGE)
        }
        recorder.shot(tti, PAGE)
    }

    private fun completedRecord(id: String) =
        TtiRecord(
            tti = Tti(id),
            pageName = PAGE,
            createdAt = 0,
            spans = TtiTimeline.entries.associateWith { TtiSpan(startedAt = 0, endedAt = 1) },
        )

    private fun kotlinx.coroutines.test.TestScope.recorder(
        store: TtiRecordStore,
        shooter: TtiShooter,
        clock: TtiClock = FakeClock(),
    ): TtiRecorder = createTtiRecorder(store, shooter, clock, StandardTestDispatcher(testScheduler))

    private companion object {
        const val PAGE = "challenge_detail"
    }
}

private class FakeClock : TtiClock {
    var elapsed: Long = 0
    var wall: Long = 0

    override fun elapsedMillis(): Long = elapsed

    override fun wallTimeMillis(): Long = wall
}

private class RecordingShooter : TtiShooter {
    val shot = mutableListOf<List<TtiRecord>>()

    override suspend fun shoot(records: List<TtiRecord>) {
        shot += records
    }
}

/** 처음 한 번만 던지고 그 뒤로는 정상 동작한다 — 실패가 기록기를 멈추지 않는지 본다. */
private class ThrowingOnceStore : TtiRecordStore {
    private val delegate = FakeStore()
    private var thrown = false

    override suspend fun openSpan(
        tti: Tti,
        pageName: String,
        createdAt: Long,
        timeline: TtiTimeline,
        startedAt: Long,
    ) {
        if (!thrown) {
            thrown = true
            error("저장 실패")
        }
        delegate.openSpan(tti, pageName, createdAt, timeline, startedAt)
    }

    override suspend fun closeSpan(
        tti: Tti,
        timeline: TtiTimeline,
        endedAt: Long,
    ) = delegate.closeSpan(tti, timeline, endedAt)

    override suspend fun find(tti: Tti): TtiRecord? = delegate.find(tti)

    override suspend fun findAll(): List<TtiRecord> = delegate.findAll()

    override suspend fun delete(ttis: List<Tti>) = delegate.delete(ttis)

    override suspend fun deleteCreatedBefore(threshold: Long) = delegate.deleteCreatedBefore(threshold)
}

/** 규칙(처음 값을 지킨다)을 실제 저장소와 같게 흉내 낸다. */
private class FakeStore : TtiRecordStore {
    val records = mutableMapOf<String, TtiRecord>()

    override suspend fun openSpan(
        tti: Tti,
        pageName: String,
        createdAt: Long,
        timeline: TtiTimeline,
        startedAt: Long,
    ) {
        val existing = records[tti.id] ?: TtiRecord(tti, pageName, createdAt, emptyMap())
        if (existing.spans.containsKey(timeline)) {
            records[tti.id] = existing
            return
        }
        records[tti.id] = existing.copy(spans = existing.spans + (timeline to TtiSpan(startedAt)))
    }

    override suspend fun closeSpan(
        tti: Tti,
        timeline: TtiTimeline,
        endedAt: Long,
    ) {
        val existing = records[tti.id] ?: return
        val span = existing.spans[timeline] ?: return
        if (span.endedAt != null) return
        records[tti.id] = existing.copy(spans = existing.spans + (timeline to span.copy(endedAt = endedAt)))
    }

    override suspend fun find(tti: Tti): TtiRecord? = records[tti.id]

    override suspend fun findAll(): List<TtiRecord> = records.values.toList()

    override suspend fun delete(ttis: List<Tti>) {
        ttis.forEach { records.remove(it.id) }
    }

    override suspend fun deleteCreatedBefore(threshold: Long) {
        records.values.filter { it.createdAt < threshold }.forEach { records.remove(it.tti.id) }
    }
}
