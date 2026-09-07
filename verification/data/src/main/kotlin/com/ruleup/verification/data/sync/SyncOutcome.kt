package com.ruleup.verification.data.sync

import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.SyncPayloadTooLargeException
import com.ruleup.verification.domain.entity.SyncTooFrequentException

/**
 * Worker 결과 판정(순수, 테스트 가능). WorkManager [androidx.work.ListenableWorker.Result] 매핑 전 단계.
 */
internal enum class SyncOutcome {
    // 정상(또는 보낼 게 없음) → Result.success
    SUCCESS,

    // 400, 그리고 더 못 쪼개는 413: 배치 폐기(usecase 가 markSynced 처리) → Result.success, 무한 재전송 금지
    DISCARD,

    // 429/네트워크 등 일시 오류 → Result.retry(백오프)
    RETRY,
}

internal fun syncOutcomeFor(error: Throwable?): SyncOutcome =
    when (error) {
        null -> SyncOutcome.SUCCESS
        is InvalidSignalPayloadException -> SyncOutcome.DISCARD
        // 분할까지 했는데도 상한을 넘었다 — 다음 주기에 같은 배치를 보내도 결과가 같다.
        is SyncPayloadTooLargeException -> SyncOutcome.DISCARD
        is SyncTooFrequentException -> SyncOutcome.RETRY
        else -> SyncOutcome.RETRY
    }
