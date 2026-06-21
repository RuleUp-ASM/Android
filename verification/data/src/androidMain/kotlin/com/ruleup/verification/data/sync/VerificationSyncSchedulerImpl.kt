package com.ruleup.verification.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.ruleup.verification.domain.port.SyncScheduler
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.TimeUnit

/**
 * WorkManager PeriodicWork 로 30분 주기 sync 를 예약한다(명세 §3.1).
 * exact 알람을 쓰지 않아 Doze/App Standby 를 견딘다. 네트워크 connected 제약만 건다(인증 적시성).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class VerificationSyncSchedulerImpl(
    private val context: Context,
) : SyncScheduler {
    override fun ensureScheduled() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            VerificationSyncWorker.WORK_NAME,
            // 이미 예약돼 있으면 유지(앱 시작마다 리셋하지 않음).
            ExistingPeriodicWorkPolicy.KEEP,
            buildRequest(DEFAULT_INTERVAL_MIN),
        )
    }

    override fun reschedule(nextSyncAfterSec: Int) {
        // WorkManager 최소 주기 15분 floor 적용.
        val minutes = (nextSyncAfterSec / SECONDS_PER_MINUTE).coerceAtLeast(MIN_INTERVAL_MIN)
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            VerificationSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            buildRequest(minutes),
        )
    }

    private fun buildRequest(intervalMinutes: Long): PeriodicWorkRequest =
        PeriodicWorkRequest
            .Builder(VerificationSyncWorker::class.java, intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            ).build()

    companion object {
        private const val DEFAULT_INTERVAL_MIN = 30L
        private const val MIN_INTERVAL_MIN = 15L
        private const val SECONDS_PER_MINUTE = 60L
    }
}
