package com.ruleup.verification.data.signal.common

import com.ruleup.verification.data.db.common.SignalGapDao
import com.ruleup.verification.data.db.common.SignalGapEntity
import com.ruleup.verification.domain.entity.GapReason
import javax.inject.Inject

/**
 * 수집기가 신호 공백 사유를 버퍼에 적재하는 진입점 (전송 스펙 §0.5).
 *
 * HC quota 초과·provider 미설치·UsageStats purge·지오펜스 미등록처럼 "신호를 못 받은 구간"을
 * 발생 즉시 기록해 두면, 다음 flush 때 신호 배치와 함께 envelope `gaps[]` 로 전송된다.
 * 서버는 이 사유를 보고 신호 부재를 NO_SIGNAL 로 단정하기 전에 PERMISSION/일시오류를 구분한다.
 */
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
