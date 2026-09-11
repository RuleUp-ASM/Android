package com.ruleup.support.domain.repository

import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.entity.InquirySummary

/**
 * 앱 내 문의. **이 경로가 유일한 창구다** — 이메일·SNS 등 외부 채널은 운영하지 않는다.
 *
 * 모든 메서드는 실패 시 `InquiryException` 을 던진다.
 */
interface InquiryRepository {
    /**
     * 문의를 접수한다(명세 POST /api/v1/inquiries).
     *
     * 자동 첨부 4종은 구현이 [DeviceContextProvider] 로 채운다 — 호출부가 넘기지 않는다.
     *
     * **접수 후 수정·삭제 경로가 없다.** 재시도는 같은 내용을 한 건 더 쌓고 하루 상한
     * (`InquiryLimits.DAILY_LIMIT`)을 함께 깎으므로, 화면은 응답을 받기 전 버튼을 잠가야 한다.
     */
    suspend fun submit(submission: InquirySubmission): InquiryReceipt

    /** 내가 접수한 문의 목록(명세 GET /api/v1/inquiries). **최신순**이고 페이징 인자가 없다. */
    suspend fun getInquiries(): List<InquirySummary>

    /**
     * 문의 1건(명세 GET /api/v1/inquiries/{inquiryId}).
     *
     * 남의 문의는 404 다 — 403 을 주면 그 번호의 문의가 존재한다는 사실이 새어 나가므로 서버가
     * 일부러 구분하지 않는다. 화면도 "없는 문의"로만 안내한다.
     */
    suspend fun getInquiry(inquiryId: String): InquiryDetail

    /**
     * 첨부 사진을 올리고 접수에 실을 주소를 받는다.
     *
     * 문의 전용 업로드 경로가 명세에 없어 **이의 제기 업로드(POST /api/v1/appeals/images)를
     * 재사용한다**(2026-09-11 합의). 서버가 용도별 소유권을 검증하기 시작하면 접수에서 거절되므로,
     * 그때 전용 경로를 받아 구현만 갈아 끼운다.
     *
     * @param imageUri 기기 로컬 content URI
     */
    suspend fun uploadImage(imageUri: String): String
}
