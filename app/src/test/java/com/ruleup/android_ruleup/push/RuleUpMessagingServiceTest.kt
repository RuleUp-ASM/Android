package com.ruleup.android_ruleup.push

import android.app.Application
import androidx.work.Configuration
import androidx.work.NetworkType
import androidx.work.WorkManager
import com.google.firebase.messaging.RemoteMessage
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.verification.data.sync.VerificationSyncSchedulerImpl
import com.ruleup.verification.domain.repository.SyncScheduler
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [33])
class RuleUpMessagingServiceTest {
    @Test
    fun `sync 요청 푸시를 받으면 네트워크가 연결된 경우 실행할 작업이 등록된다`() {
        val context = RuntimeEnvironment.getApplication()
        WorkManager.initialize(context, Configuration.Builder().build())
        val manager = WorkManager.getInstance(context)
        val service =
            RuleUpMessagingService().apply {
                syncScheduler = VerificationSyncSchedulerImpl(context)
                observability = testObservability()
            }

        service.onMessageReceived(RemoteMessage.Builder("test").addData("type", "SYNC_REQUIRED").build())

        val work = manager.getWorkInfosForUniqueWork(VerificationSyncSchedulerImpl.CATCH_UP_WORK_NAME).get(10, TimeUnit.SECONDS).single()
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    @Test
    fun `sync 요청 푸시는 알림 없이 catch-up 동기화를 예약한다`() {
        val scheduler = FakeSyncScheduler()
        val service =
            RuleUpMessagingService().apply {
                syncScheduler = scheduler
                observability = testObservability()
            }
        val message = RemoteMessage.Builder("test").addData("type", "SYNC_REQUIRED").build()

        service.onMessageReceived(message)

        assertEquals(1, scheduler.catchUpCount)
    }

    private class FakeSyncScheduler : SyncScheduler {
        var catchUpCount = 0

        override fun enqueueCatchUp() {
            catchUpCount++
        }

        override fun cancel() = Unit

        override fun ensureScheduled() = Unit

        override fun reschedule(flushIntervalSec: Int) = Unit
    }
}
