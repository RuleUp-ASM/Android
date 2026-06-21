package com.ruleup.verification.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ruleup.verification.domain.port.ProgressCacheStore
import com.ruleup.verification.domain.port.SyncScheduler
import com.ruleup.verification.domain.port.SyncScopeProvider
import com.ruleup.verification.domain.usecase.RunSyncUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 30분 주기 sync 실행기(명세 §3.1). Metro 가 주입한 의존성을 [VerificationWorkerFactory] 가 넘긴다.
 * 결과 매핑: 성공/폐기(400)→success, 429/일시오류→retry(백오프). 멱등 키 collectedAt 는 매 실행 stamp.
 */
class VerificationSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val runSyncUseCase: RunSyncUseCase,
    private val syncScopeProvider: SyncScopeProvider,
    private val progressCacheStore: ProgressCacheStore,
    private val syncScheduler: SyncScheduler,
) : CoroutineWorker(appContext, params) {
    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result {
        val scope = syncScopeProvider.currentScope()
        val collectedAt = Clock.System.now().toString()
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
