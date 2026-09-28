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

/** 기록기 회귀 테스트. */
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

    /** 이벤트 전송 순서 보장. */
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
            // 셋 다 아직 전송 중이다.
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

    /** 컨텍스트 조회 실패 격리. */
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

/** 손으로 감는 시계. */
private class ManualClock(
    var now: Long = 0L,
) : BizLogClock {
    override fun nowMillis(): Long = now
}

/** 전송 도중 양보하는 테스트 대역. */
private class RecordingShooter : BizLogShooter {
    private val _received = mutableListOf<BizLog>()

    /** 전송 큐를 비운 뒤 조회. */
    val received: List<BizLog> get() = _received
    var failing = false

    override suspend fun shoot(log: BizLog) {
        yield()
        if (failing) throw IllegalStateException("network down")
        _received += log
    }
}
