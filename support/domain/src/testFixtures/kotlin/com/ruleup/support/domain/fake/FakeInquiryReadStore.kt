package com.ruleup.support.domain.fake

import com.ruleup.support.domain.repository.InquiryReadStore

/** 메모리에만 보관하는 확인 기록. */
class FakeInquiryReadStore(
    seen: Map<String, String> = emptyMap(),
) : InquiryReadStore {
    val seen = seen.toMutableMap()

    override suspend fun seenAnswers(): Map<String, String> = seen.toMap()

    override suspend fun markSeen(
        inquiryId: String,
        answeredAt: String,
    ) {
        seen[inquiryId] = answeredAt
    }
}
