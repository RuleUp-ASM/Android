package com.ruleup.support.domain.repository

/**
 * 이 기기에서 확인한 문의 답변을 보관한다.
 *
 * 서버 응답에 읽음 필드가 없어 기기 단위다(#559). 재설치하거나 다른 기기에서 보면 다시 새 답변으로 보인다.
 */
interface InquiryReadStore {
    /** 문의 id → 확인 당시 답변 시각(ISO-8601). */
    suspend fun seenAnswers(): Map<String, String>

    suspend fun markSeen(
        inquiryId: String,
        answeredAt: String,
    )
}
