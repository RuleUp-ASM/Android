package com.ruleup.verification.data.sync

import com.ruleup.verification.domain.entity.InvalidSignalPayloadException
import com.ruleup.verification.domain.entity.SyncTooFrequentException
import kotlin.test.Test
import kotlin.test.assertEquals

class SyncOutcomeTest {
    @Test
    fun `정상은 SUCCESS`() {
        assertEquals(SyncOutcome.SUCCESS, syncOutcomeFor(null))
    }

    @Test
    fun `400 INVALID_SIGNAL_PAYLOAD 은 DISCARD - 폐기 후 success`() {
        assertEquals(SyncOutcome.DISCARD, syncOutcomeFor(InvalidSignalPayloadException()))
    }

    @Test
    fun `429 SYNC_TOO_FREQUENT 은 RETRY - 백오프`() {
        assertEquals(SyncOutcome.RETRY, syncOutcomeFor(SyncTooFrequentException()))
    }

    @Test
    fun `기타 일시 오류는 RETRY`() {
        assertEquals(SyncOutcome.RETRY, syncOutcomeFor(RuntimeException("network")))
    }
}
