package com.ruleup.verification.data.sync

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.ruleup.verification.domain.port.ProgressCacheStore
import com.ruleup.verification.domain.port.SyncScheduler
import com.ruleup.verification.domain.port.SyncScopeProvider
import com.ruleup.verification.domain.usecase.RunSyncUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding

/**
 * Metro 가 주입한 의존성으로 [VerificationSyncWorker] 를 생성하는 WorkerFactory(명세 §3, Hilt 대체).
 * :app 의 WorkManager Configuration 에 등록된다. 다른 Worker 는 null 을 반환해 기본 인스턴스화에 위임한다.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<WorkerFactory>())
class VerificationWorkerFactory(
    private val runSyncUseCase: RunSyncUseCase,
    private val syncScopeProvider: SyncScopeProvider,
    private val progressCacheStore: ProgressCacheStore,
    private val syncScheduler: SyncScheduler,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? =
        when (workerClassName) {
            VerificationSyncWorker::class.java.name ->
                VerificationSyncWorker(
                    appContext = appContext,
                    params = workerParameters,
                    runSyncUseCase = runSyncUseCase,
                    syncScopeProvider = syncScopeProvider,
                    progressCacheStore = progressCacheStore,
                    syncScheduler = syncScheduler,
                )

            else -> null
        }
}
