package com.ruleup.verification.data.sync

import android.app.Application
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class VerificationSyncSchedulerTest {
    @Test
    fun `서버 대기 시간과 네트워크 조건으로 예약하고 동기화 취소 시 함께 취소한다`() =
        runBlocking {
            val context = RuntimeEnvironment.getApplication()
            WorkManager.initialize(context, Configuration.Builder().build())
            val manager = WorkManager.getInstance(context)

            VerificationSyncSchedulerImpl.enqueueDelayedCatchUp(context, 123)

            val work = manager.getWorkInfosByTag(VerificationSyncWorker::class.java.name).get(10, TimeUnit.SECONDS).single()
            assertEquals(WorkInfo.State.ENQUEUED, work.state)
            assertEquals(123_000L, work.initialDelayMillis)
            assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)

            VerificationSyncSchedulerImpl.enqueueDelayedCatchUp(context, 45)
            val queued = manager.getWorkInfosByTag(VerificationSyncWorker::class.java.name).get(10, TimeUnit.SECONDS)
            assertEquals(2, queued.size)
            val successor = queued.single { it.id != work.id }
            assertEquals(WorkInfo.State.BLOCKED, successor.state)
            assertEquals(45_000L, successor.initialDelayMillis)

            VerificationSyncSchedulerImpl(context).cancel()
            val cancelled = withTimeout(10_000) { manager.getWorkInfoByIdFlow(work.id).first { it?.state == WorkInfo.State.CANCELLED } }
            assertEquals(WorkInfo.State.CANCELLED, cancelled?.state)
            val successorCancelled =
                withTimeout(10_000) { manager.getWorkInfoByIdFlow(successor.id).first { it?.state == WorkInfo.State.CANCELLED } }
            assertEquals(WorkInfo.State.CANCELLED, successorCancelled?.state)
        }
}
