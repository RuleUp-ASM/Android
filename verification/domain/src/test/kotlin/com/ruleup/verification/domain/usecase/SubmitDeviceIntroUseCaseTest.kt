package com.ruleup.verification.domain.usecase

import com.ruleup.verification.domain.entity.DeviceIntro
import com.ruleup.verification.domain.entity.DeviceProfile
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.entity.SyncPolicy
import com.ruleup.verification.domain.repository.DeviceIntroProvider
import com.ruleup.verification.domain.repository.SyncPolicyStore
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.test.FakeVerificationRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** 로그인 직후 1회 도는 인트로. */
class SubmitDeviceIntroUseCaseTest {
    @Test
    fun `기기 프로필을 보내고 받은 정책을 저장한다`() =
        runBlocking {
            val store = RecordingPolicyStore()
            useCase(store = store)()

            assertEquals(600, store.saved?.flushIntervalSec)
        }

    @Test
    fun `저장한 뒤 그 간격으로 다음 주기를 다시 잡는다`() =
        runBlocking {
            // 순서가 뒤집히면 낡은 간격으로 재예약된다.
            val order = mutableListOf<String>()
            val store = RecordingPolicyStore(order)
            val scheduler = RecordingScheduler(order)

            useCase(store = store, scheduler = scheduler)()

            assertEquals(listOf("save", "reschedule"), order)
            assertEquals(600, scheduler.rescheduledTo)
        }

    @Test
    fun `전송에 실패하면 정책을 건드리지 않고 실패를 그대로 올린다`() =
        runBlocking {
            // 실패했는데 저장·재예약이 돌면 받은 적 없는 정책으로 주기가 바뀐다.
            val store = RecordingPolicyStore()
            val scheduler = RecordingScheduler()

            assertFailsWith<IllegalStateException> {
                useCase(store = store, scheduler = scheduler, failSubmit = true)()
            }

            assertTrue(store.saved == null)
            assertTrue(scheduler.rescheduledTo == null)
        }

    private fun useCase(
        store: SyncPolicyStore = RecordingPolicyStore(),
        scheduler: SyncScheduler = RecordingScheduler(),
        failSubmit: Boolean = false,
    ) = SubmitDeviceIntroUseCase(
        deviceIntroProvider = DeviceIntroProvider { intro() },
        verificationRepository =
            FakeVerificationRepository(
                submitIntro = { if (failSubmit) throw IllegalStateException("전송 실패") else policy() },
            ),
        syncPolicyStore = store,
        syncScheduler = scheduler,
    )

    private fun intro() =
        DeviceIntro(
            profile = DeviceProfile(sdkInt = 34, model = "Pixel", lowRam = false, appVersion = "1.0.0"),
            permissions = snapshot(),
        )

    private fun policy() =
        SyncPolicy(
            flushIntervalSec = 600,
            geofence = null,
            screenTime = null,
            wake = null,
            health = null,
            backoff = null,
            sessionId = null,
        )

    private fun snapshot() =
        PermissionSnapshot(
            location = PermissionState.GRANTED,
            backgroundLocation = PermissionState.GRANTED,
            usageStats = PermissionState.GRANTED,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )
}

/** 저장 시점을 [order] 에 남긴다 */
private class RecordingPolicyStore(
    private val order: MutableList<String> = mutableListOf(),
) : SyncPolicyStore {
    var saved: SyncPolicy? = null
        private set

    override suspend fun save(policy: SyncPolicy) {
        order += "save"
        saved = policy
    }
}

private class RecordingScheduler(
    private val order: MutableList<String> = mutableListOf(),
) : SyncScheduler {
    var rescheduledTo: Int? = null
        private set

    override fun cancel() = Unit

    override fun ensureScheduled() = throw NotImplementedError()

    override fun enqueueCatchUp() = throw NotImplementedError()

    override fun reschedule(flushIntervalSec: Int) {
        order += "reschedule"
        rescheduledTo = flushIntervalSec
    }
}
