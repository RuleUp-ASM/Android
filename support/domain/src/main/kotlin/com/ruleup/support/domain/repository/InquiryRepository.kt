package com.ruleup.support.domain.repository

import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.entity.InquirySummary

/** 앱 내 문의. */
interface InquiryRepository {
    /** 문의를 접수한다. */
    suspend fun submit(submission: InquirySubmission): InquiryReceipt

    /** 내가 접수한 문의 목록. */
    suspend fun getInquiries(): List<InquirySummary>

    /** 문의 1건. */
    suspend fun getInquiry(inquiryId: String): InquiryDetail

    /** 첨부 사진을 올리고 접수에 실을 주소를 받는다. */
    suspend fun uploadImage(imageUri: String): String
}
