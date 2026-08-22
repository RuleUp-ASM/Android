package com.ruleup.verification.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.ruleup.verification.domain.repository.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * WorkManager PeriodicWork 로 30분 주기 sync 를 예약한다(명세 §3.1).
 * exact 알람을 쓰지 않아 Doze/App Standby 를 견딘다. 네트워크 connected 제약만 건다(인증 적시성).
 */
class VerificationSyncSchedulerImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SyncScheduler {
        override fun ensureScheduled() {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                VerificationSyncWorker.WORK_NAME,
                // 이미 예약돼 있으면 유지(앱 시작마다 리셋하지 않음).
                ExistingPeriodicWorkPolicy.KEEP,
                buildRequest(DEFAULT_INTERVAL_MIN),
            )
        }

        override fun reschedule(flushIntervalSec: Int) {
            // WorkManager 최소 주기 15분 floor 적용.
            val minutes = (flushIntervalSec / SECONDS_PER_MINUTE).coerceAtLeast(MIN_INTERVAL_MIN)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                VerificationSyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                buildRequest(minutes),
            )
        }

        override fun enqueueCatchUp() = enqueueCatchUp(context)

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
            const val CATCH_UP_WORK_NAME = "verification_sync_catchup"

            /**
             * push 트리거용 expedited catch-up. Hilt 그래프에 접근 못 하는 BroadcastReceiver 도
             * WorkManager 싱글톤으로 직접 호출할 수 있도록 static 으로 노출한다(전송 스펙 §0.6).
             *
             * 기본 [ExistingWorkPolicy.KEEP] 은 연쇄 발화 시 폭주를 막는다(운영 경로). 디버그의 수동 트리거처럼
             * "지금 새로 돌려라"가 필요하면 [ExistingWorkPolicy.REPLACE] 로 대기/재시도 중인 작업을 갈아끼운다.
             */
            fun enqueueCatchUp(
                context: Context,
                policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
            ) {
                val request =
                    OneTimeWorkRequest
                        .Builder(VerificationSyncWorker::class.java)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setConstraints(
                            Constraints
                                .Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build(),
                        ).build()
                WorkManager.getInstance(context).enqueueUniqueWork(
                    CATCH_UP_WORK_NAME,
                    policy,
                    request,
                )
            }
        }
    }
