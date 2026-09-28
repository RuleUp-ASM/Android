package com.ruleup.verification.data.signal.common

import com.ruleup.verification.data.db.common.SignalGapDao
import com.ruleup.verification.data.db.common.SignalGapEntity
import com.ruleup.verification.domain.entity.GapReason
import javax.inject.Inject

/** 수집기가 신호 공백 사유를 버퍼에 적재하는 진입점. */
class GapRecorder
    @Inject
    constructor(
        private val gapDao: SignalGapDao,
    ) {
        suspend fun record(
            signalType: String,
            reason: GapReason,
            fromMillis: Long,
            toMillis: Long,
            recoverable: Boolean,
        ) {
            gapDao.insert(
                SignalGapEntity(
                    signalType = signalType,
                    reason = reason,
                    fromMillis = fromMillis,
                    toMillis = toMillis,
                    recoverable = recoverable,
                    occurredAt = System.currentTimeMillis(),
                ),
            )
        }
    }
