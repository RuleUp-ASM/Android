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
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.event.DiagnosticPayload
import com.ruleup.observability.domain.model.ErrorInfo
import com.ruleup.observability.domain.model.Severity
import com.ruleup.observability.domain.model.attributes
import com.ruleup.verification.data.settings.VerificationSettingsStore
import com.ruleup.verification.domain.repository.ProgressCacheStore
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.repository.SyncScopeProvider
import com.ruleup.verification.domain.usecase.RunSyncUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
        private val syncGate: SyncGate,
        private val observability: Observability,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            // 주기 work 와 catch-up 은 unique name 이 달라 WorkManager 가 겹침을 막지 못한다(#355).
            // 겹치면 뒤 실행의 tagPending 이 앞 실행의 배치를 덮어써 같은 신호가 두 번 나간다.
            if (!syncGate.tryEnter()) {
                // 버리지 않고 재시도한다 — catch-up 의 존재 이유가 전달 지연 단축이라, 겹쳤다고
                // 다음 주기(최대 30분)까지 미루면 목적이 사라진다.
                observability.i(LOG_TAG) { "sync 건너뜀 — 다른 실행이 드레인 중, 백오프 재시도" }
                return Result.retry()
            }
            return try {
                runSync()
            } finally {
                syncGate.leave()
            }
        }

        private suspend fun runSync(): Result {
            val scope = syncScopeProvider.currentScope()
            // 디버그 가시화: 이번 sync 의 수집 스코프(타깃이 비면 수집기가 전부 생략됨).
            observability.i(LOG_TAG) {
                "sync 시작 — scope: geofence=${scope.activeRequestIds.size}, " +
                    "usage=${scope.targetPackages.size}, health=${scope.healthTargets.size}, " +
                    "sleep=${scope.sleepRequested}"
            }
            val collectedAt = Instant.now().toString()
            return try {
                val result = runSyncUseCase(scope, collectedAt)
                if (result != null) {
                    // updatedChallenges → 진행률 캐시, flushIntervalSec → 다음 주기 동적 조정.
                    progressCacheStore.upsert(result.updatedChallenges)
                    syncScheduler.reschedule(result.flushIntervalSec)
                    // 진단 heartbeat 앵커(전송 스펙 §0.7) — 다음 envelope 에 마지막 성공 flush 시각으로 동봉.
                    settingsStore.setLastSuccessfulFlushAt(System.currentTimeMillis())
                    observability.i(LOG_TAG) {
                        "sync 성공 — 갱신=${result.updatedChallenges.size}, " +
                            "무시타입=${result.ignoredSignalTypes}, next=${result.flushIntervalSec}s, " +
                            "상한=${result.maxPayloadBytes ?: "미수신"}"
                    }
                } else {
                    // 활성 챌린지도 신호·gap 도 없어 전송 생략.
                    observability.i(LOG_TAG) { "sync 전송 생략 — 활성 챌린지·신호·gap 0" }
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val outcome = syncOutcomeFor(e)
                // 처리된 실패도 기기별 원인 파악을 위해 관측한다. 진단 채널 → CrashlyticsSink 로 non-fatal 기록.
                observability.log(Channel.DIAGNOSTIC, Severity.ERROR, LOG_TAG) {
                    DiagnosticPayload(
                        severity = Severity.ERROR,
                        tag = LOG_TAG,
                        message = "sync 실패",
                        cause = ErrorInfo.from(e),
                        attrs = attributes { put("sync_outcome", outcome.name) },
                    )
                }
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
