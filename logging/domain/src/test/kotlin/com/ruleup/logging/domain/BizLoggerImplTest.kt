package com.ruleup.logging.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 기록기 회귀 테스트.
 *
 * 기록기는 아무것도 돌려주지 않으므로 "언제 나갔는가" 를 밖에서 알 방법이 없다. 그래서 테스트
 * 스케줄러를 물리고 [advanceUntilIdle] 로 큐를 비운 다음에 본다 — 실제 시간을 두고 기다리면 느린
 * 기계에서 아직 처리되지 않은 상태를 결과로 읽는다.
 *
 * 가짜 전송기는 매번 [yield] 해, 전송이 왕복하는 사이에 다음 건이 끼어들 틈을 일부러 만든다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BizLoggerImplTest {
    @Test
    fun `기록한 건이 화면과 사용자를 달고 나간다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter, screen = "/challenge/detail", user = "u1")
            logger.init()

            logger.record(event("challenge_join_attempt"))
            advanceUntilIdle()

            val log = shooter.received.single()
            assertEquals("challenge_join_attempt", log.event.name)
            assertEquals("/challenge/detail", log.screen)
            assertEquals("u1", log.userId)
        }

    /** 전송이 중간에 멈추므로, 순서를 묶어 두지 않으면 나중 건이 앞질러 나간다. */
    @Test
    fun `여러 건을 기록하면 일어난 순서대로 나간다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter)
            logger.init()

            listOf("a", "b", "c").forEach { logger.record(event(it)) }
            advanceUntilIdle()

            assertEquals(listOf("a", "b", "c"), shooter.received.map { it.event.name })
        }

    @Test
    fun `화면이 바뀌어도 앞서 기록된 건은 일어난 화면을 단다`() =
        runTest {
            // 전송이 IO 로 넘어가 나중에 도는 사이 사용자가 다음 화면으로 넘어갈 수 있다.
            val shooter = RecordingShooter()
            var screen = "/explore"
            val logger = logger(shooter, screenSource = { screen })
            logger.init()

            logger.record(event("explore_home_view"))
            screen = "/challenge/detail"
            advanceUntilIdle()

            assertEquals("/explore", shooter.received.single().screen)
        }

    @Test
    fun `로그인 전이면 사용자 없이 나간다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter, user = null)
            logger.init()

            logger.record(event("login_screen_view"))
            advanceUntilIdle()

            assertEquals(null, shooter.received.single().userId)
        }

    @Test
    fun `init 전에 기록한 것은 버린다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter)

            logger.record(event("explore_home_view"))
            advanceUntilIdle()

            assertTrue(shooter.received.isEmpty())
        }

    @Test
    fun `destroy 는 얹혀 있던 전송을 끝내고 접는다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter)
            logger.init()

            listOf("a", "b", "c").forEach { logger.record(event(it)) }
            // 셋 다 아직 전송 중이다. 여기서 스코프를 먼저 접으면 남은 것이 취소된다.
            logger.destroy()
            advanceUntilIdle()

            assertEquals(3, shooter.received.size)
        }

    @Test
    fun `destroy 뒤의 기록은 버리고 다시 init 하면 나간다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter)
            logger.init()
            logger.destroy()
            advanceUntilIdle()

            logger.record(event("버려짐"))
            advanceUntilIdle()
            assertTrue(shooter.received.isEmpty())

            // 앱이 다시 앞으로 나왔다.
            logger.init()
            logger.record(event("explore_home_view"))
            advanceUntilIdle()

            assertEquals(
                "explore_home_view",
                shooter.received
                    .single()
                    .event.name,
            )
        }

    @Test
    fun `전송이 실패해도 다음 건은 나간다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter)
            logger.init()

            shooter.failing = true
            logger.record(event("잃어버림"))
            advanceUntilIdle()
            assertTrue(shooter.received.isEmpty())

            shooter.failing = false
            logger.record(event("explore_home_view"))
            advanceUntilIdle()

            assertEquals(
                "explore_home_view",
                shooter.received
                    .single()
                    .event.name,
            )
        }

    /** 화면·사용자 출처가 던져도 기록은 계속돼야 한다 — 로그가 앱을 멈추게 하는 일은 없어야 한다. */
    @Test
    fun `화면 출처가 던져도 화면 없이 기록한다`() =
        runTest {
            val shooter = RecordingShooter()
            val logger = logger(shooter, screenSource = { error("내비게이션 상태 없음") })
            logger.init()

            logger.record(event("explore_home_view"))
            advanceUntilIdle()

            assertEquals(null, shooter.received.single().screen)
        }

    @Test
    fun `기록 시각은 시계가 가리키는 때다`() =
        runTest {
            val shooter = RecordingShooter()
            val clock = ManualClock()
            val logger = logger(shooter, clock = clock)
            logger.init()

            logger.record(event("a"))
            clock.now += 50L
            logger.record(event("b"))
            advanceUntilIdle()

            val recordedAt = shooter.received.map { it.recordedAt }
            assertEquals(50L, recordedAt[1] - recordedAt[0])
        }

    private fun TestScope.logger(
        shooter: BizLogShooter,
        clock: BizLogClock = ManualClock(),
        screen: String? = "/explore",
        user: String? = "u1",
        screenSource: BizScreenSource = BizScreenSource { screen },
    ): BizLogger =
        BizLoggerImpl(
            shooter = shooter,
            clock = clock,
            screenSource = screenSource,
            userSource = { user },
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

    private fun event(name: String) = BizEvent(name, bizAttributes { put("k", "v") })
}

/** 손으로 감는 시계. 흐르게 하지 않으면 멈춰 있다. */
private class ManualClock(
    var now: Long = 0L,
) : BizLogClock {
    override fun nowMillis(): Long = now
}

/** 매번 한 번 양보해, 전송이 왕복하는 사이에 다음 건이 끼어들 틈을 만든다. */
private class RecordingShooter : BizLogShooter {
    private val _received = mutableListOf<BizLog>()

    /** [advanceUntilIdle] 로 큐를 비운 뒤에 본다 — 그 시점에는 더 들어올 것이 없다. */
    val received: List<BizLog> get() = _received
    var failing = false

    override suspend fun shoot(log: BizLog) {
        yield()
        if (failing) throw IllegalStateException("network down")
        _received += log
    }
}
