package com.ruleup.verification.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ruleup.verification.domain.port.ProgressCacheStore
import com.ruleup.verification.domain.port.SyncScheduler
import com.ruleup.verification.domain.port.SyncScopeProvider
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
    ) : CoroutineWorker(appContext, params) {
        override suspend fun doWork(): Result {
            val scope = syncScopeProvider.currentScope()
            val collectedAt = Instant.now().toString()
            return try {
                val result = runSyncUseCase(scope, collectedAt)
                if (result != null) {
                    // updatedChallenges → 진행률 캐시, nextSyncAfterSec → 다음 주기 동적 조정.
                    progressCacheStore.upsert(result.updatedChallenges)
                    syncScheduler.reschedule(result.nextSyncAfterSec)
                }
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (syncOutcomeFor(e)) {
                    SyncOutcome.SUCCESS, SyncOutcome.DISCARD -> Result.success()
                    SyncOutcome.RETRY -> Result.retry()
                }
            }
        }

        companion object {
            const val WORK_NAME = "verification_sync"
        }
    }
