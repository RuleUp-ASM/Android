package com.ruleup.verification.data.sync

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.ruleup.analytics.AnalyticsEvent
import com.ruleup.analytics.AnalyticsLogger
import com.ruleup.analytics.CrashReporter
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.port.ProgressCacheStore
import com.ruleup.verification.domain.port.SyncScheduler
import com.ruleup.verification.domain.port.SyncScopeProvider
import com.ruleup.verification.domain.usecase.RunSyncUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/**
 * 30분 주기 sync 실행기(명세 §3.1). Hilt(@HiltWorker)가 의존성을 주입하고
 * HiltWorkerFactory 가 인스턴스화한다.
 * 결과 매핑: 성공/폐기(400)→success, 429/일시오류→retry(백오프). 멱등 키 collectedAt 는 매 실행 stamp.
 */
@HiltWorker
class VerificationSyncWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val runSyncUseCase: RunSyncUseCase,
        private val syncScopeProvider: SyncScopeProvider,
        private val progressCacheStore: ProgressCacheStore,
        private val syncScheduler: SyncScheduler,
        private val settingsStore: VerificationSettingsStore,
        private val analyticsLogger: AnalyticsLogger,
        private val crashReporter: CrashReporter,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val scope = syncScopeProvider.currentScope()
            // 디버그 가시화: 이번 sync 의 수집 스코프(타깃이 비면 수집기가 전부 생략됨).
            Timber.tag(LOG_TAG).i(
                "sync 시작 — scope: geofence=%d, usage=%d, health=%d, sleep=%b",
                scope.activeRequestIds.size,
                scope.targetPackages.size,
                scope.healthTargets.size,
                scope.sleepRequested,
            )
            val collectedAt = Instant.now().toString()
            return try {
                val result = runSyncUseCase(scope, collectedAt)
                if (result != null) {
                    // updatedChallenges → 진행률 캐시, nextSyncAfterSec → 다음 주기 동적 조정.
                    progressCacheStore.upsert(result.updatedChallenges)
                    syncScheduler.reschedule(result.nextSyncAfterSec)
                    // 진단 heartbeat 앵커(전송 스펙 §0.7) — 다음 envelope 에 마지막 성공 flush 시각으로 동봉.
                    settingsStore.setLastSuccessfulFlushAt(System.currentTimeMillis())
                    Timber.tag(LOG_TAG).i(
                        "sync 성공 — 갱신=%d, 무시타입=%s, next=%ds",
                        result.updatedChallenges.size,
                        result.ignoredSignalTypes,
                        result.nextSyncAfterSec,
                    )
                    analyticsLogger.log(
                        AnalyticsEvent.VerificationSynced(
                            updatedCount = result.updatedChallenges.size,
                            ignoredCount = result.ignoredSignalTypes.size,
                        ),
                    )
                } else {
                    // 신호도 gap 도 없어 전송 생략(전송 스펙 §0.2).
                    Timber.tag(LOG_TAG).i("sync 전송 생략 — 수집 신호·gap 0 (스코프/권한 확인)")
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val outcome = syncOutcomeFor(e)
                Timber.tag(LOG_TAG).w(e, "sync 실패 — outcome=%s", outcome)
                // 처리된 실패도 기기별 원인 파악을 위해 관측: non-fatal 스택 + 분류 이벤트(비식별).
                crashReporter.recordException(e, mapOf("sync_outcome" to outcome.name))
                analyticsLogger.log(AnalyticsEvent.VerificationSyncFailed(reason = outcome.name))
                when (outcome) {
                    SyncOutcome.SUCCESS, SyncOutcome.DISCARD -> Result.success()
                    SyncOutcome.RETRY -> Result.retry()
                }
            }
        }

        /**
         * expedited OneTimeWork(catch-up)가 API 31 미만에서 foreground service 로 승격될 때 쓰는 알림
         * (전송 스펙 §0.6). 31+ 는 expedited job 쿼터로 돌아 알림이 표시되지 않는다. IMPORTANCE_MIN 으로 조용히.
         */
        override suspend fun getForegroundInfo(): ForegroundInfo {
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "인증 신호 동기화", NotificationManager.IMPORTANCE_MIN),
            )
            val notification =
                Notification
                    .Builder(applicationContext, CHANNEL_ID)
                    .setContentTitle("인증 신호 동기화 중")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .setOngoing(true)
                    .build()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                ForegroundInfo(NOTIFICATION_ID, notification)
            }
        }

        companion object {
            const val WORK_NAME = "verification_sync"
            private const val CHANNEL_ID = "verification_sync"
            private const val NOTIFICATION_ID = 4801

            // 수집·동기화 경로 공통 로그 태그(SignalRepositoryImpl 과 동일). 'VerifySync' 로 필터.
            private const val LOG_TAG = "VerifySync"
        }
    }
