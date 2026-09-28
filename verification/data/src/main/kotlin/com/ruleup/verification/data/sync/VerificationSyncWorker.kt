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
import com.ruleup.domain.helper.PushNotificationHelper
import com.ruleup.domain.navigation.AppRoutes
import com.ruleup.domain.navigation.NavRoute
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

/** 30분 주기 sync 실행기. */
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
        private val pushNotificationHelper: PushNotificationHelper,
        private val observability: Observability,
        private val tokenRepository: com.ruleup.domain.token.TokenRepository,
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            // 주기 work 와 catch-up 은 unique name 이 달라 WorkManager 가 겹침을 막지 못한다(#355).
            if (!syncGate.tryEnter()) {
                // 버리지 않고 재시도한다
                observability.i(LOG_TAG) { "sync 건너뜀 — 다른 실행이 드레인 중, 백오프 재시도" }
                return Result.retry()
            }
            return try {
                if (tokenRepository.getAccessToken() == null) Result.success() else runSync()
            } finally {
                syncGate.leave()
            }
        }

        private suspend fun runSync(): Result {
            val scope = syncScopeProvider.currentScope()
            // 타깃이 비면 수집기가 전부 생략된다
            observability.i(LOG_TAG) {
                "sync 시작 — scope: geofence=${scope.activeRequestIds.size}, " +
                    "usage=${scope.targetPackages.size}, health=${scope.healthTargets.size}, " +
                    "sleep=${scope.sleepRequested}"
            }
            val collectedAt = Instant.now().toString()
            return try {
                val result = runSyncUseCase(scope, collectedAt)
                if (result != null) {
                    progressCacheStore.upsert(result.updatedChallenges)
                    syncScheduler.reschedule(result.flushIntervalSec)
                    // 진단 heartbeat 앵커
                    settingsStore.setLastSuccessfulFlushAt(System.currentTimeMillis())
                    observability.i(LOG_TAG) {
                        "sync 성공 — 갱신=${result.updatedChallenges.size}, " +
                            "무시타입=${result.ignoredSignalTypes}, next=${result.flushIntervalSec}s, " +
                            "상한=${result.maxPayloadBytes ?: "미수신"}"
                    }
                    notifyConsentRequired(result.consentRequired)
                } else {
                    observability.i(LOG_TAG) { "sync 전송 생략 — 활성 챌린지·신호·gap 0" }
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val outcome = syncOutcomeFor(e)
                // 처리된 실패도 기기별 원인 파악을 위해 관측한다.
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
                    SyncOutcome.SUCCESS, SyncOutcome.STOP_RETRY -> Result.success()
                    SyncOutcome.RETRY -> Result.retry()
                }
            }
        }

        /** 개별 동의가 빠져 신호가 저장되지 않았음을 알린다. */
        private fun notifyConsentRequired(consentRequired: List<String>) {
            if (consentRequired.isEmpty()) return
            observability.i(LOG_TAG) { "개별 동의 필요 — $consentRequired" }
            pushNotificationHelper.show(
                id = CONSENT_NOTIFICATION_ID,
                title = "동의가 필요해요",
                message = "동의가 없어 인증 기록이 저장되지 않고 있어요. 확인해 주세요.",
                route = NavRoute(AppRoutes.MY_AGREEMENTS),
            )
        }

        /** API 31 미만 즉시 동기화의 포그라운드 알림. */
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

            // 수집·동기화 경로 공통 로그 태그(SignalRepositoryImpl 과 동일).
            private const val CONSENT_NOTIFICATION_ID = 90_101

            private const val LOG_TAG = "VerifySync"
        }
    }
