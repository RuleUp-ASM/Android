package com.ruleup.verification.data.sync

import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.SyncPayloadTooLargeException
import com.ruleup.verification.domain.entity.SyncTooFrequentException

/** Worker 결과 판정(순수, 테스트 가능). */
internal enum class SyncOutcome {
    // 정상(또는 보낼 게 없음) → Result.success
    SUCCESS,

    // 400·분할 불가 413은 Worker 재시도 종료.
    STOP_RETRY,

    // 429/네트워크 등 일시 오류 → Result.retry(백오프)
    RETRY,
}

internal fun syncOutcomeFor(error: Throwable?): SyncOutcome =
    when (error) {
        null -> SyncOutcome.SUCCESS
        is InvalidSignalPayloadException -> SyncOutcome.STOP_RETRY
        // 분할까지 했는데도 상한을 넘었다
        is SyncPayloadTooLargeException -> SyncOutcome.STOP_RETRY
        is SyncTooFrequentException -> SyncOutcome.RETRY
        else -> SyncOutcome.RETRY
    }
