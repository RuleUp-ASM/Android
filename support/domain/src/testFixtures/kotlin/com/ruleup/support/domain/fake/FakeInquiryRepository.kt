package com.ruleup.support.domain.fake

import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.support.domain.repository.InquiryRepository

/** 테스트용 [InquiryRepository]. */
class FakeInquiryRepository(
    private val inquiries: (() -> List<InquirySummary>)? = null,
    private val detail: ((String) -> InquiryDetail)? = null,
    private val submit: ((InquirySubmission) -> InquiryReceipt)? = null,
    private val upload: ((String) -> String)? = null,
) : InquiryRepository {
    val calls = mutableListOf<String>()

    /** 접수에 실제로 실려 간 값. */
    val submissions = mutableListOf<InquirySubmission>()

    val uploadedUris = mutableListOf<String>()

    val openedIds = mutableListOf<String>()

    override suspend fun submit(submission: InquirySubmission): InquiryReceipt {
        calls += "submit"
        submissions += submission
        val block = submit ?: error("submit 준비 안 됨")
        return block(submission)
    }

    override suspend fun getInquiries(): List<InquirySummary> {
        calls += "getInquiries"
        return inquiries?.invoke() ?: emptyList()
    }

    override suspend fun getInquiry(inquiryId: String): InquiryDetail {
        calls += "getInquiry"
        openedIds += inquiryId
        val block = detail ?: error("getInquiry 준비 안 됨")
        return block(inquiryId)
    }

    override suspend fun uploadImage(imageUri: String): String {
        calls += "uploadImage"
        uploadedUris += imageUri
        val block = upload ?: error("uploadImage 준비 안 됨")
        return block(imageUri)
    }
}

/** 목록 픽스처. */
fun inquirySummary(
    inquiryId: String = "3d6ad414-5fb6-81aa-8d11-ca6ffd272529",
    category: InquiryCategory? = InquiryCategory.VERIFICATION,
    status: InquiryStatus = InquiryStatus.RECEIVED,
    preview: String = "기상 인증이 실패로 떴어요",
    createdAt: String = "2026-09-05T14:22:00Z",
    answeredAt: String? = null,
): InquirySummary =
    InquirySummary(
        inquiryId = inquiryId,
        category = category,
        status = status,
        preview = preview,
        createdAt = createdAt,
        answeredAt = answeredAt,
    )

/** 상세 픽스처. */
fun inquiryDetail(
    inquiryId: String = "3d6ad414-5fb6-81aa-8d11-ca6ffd272529",
    category: InquiryCategory? = InquiryCategory.VERIFICATION,
    body: String = "기상 인증이 실패로 떴어요. 헬스 커넥트 권한은 허용돼 있습니다.",
    imageUrls: List<String> = emptyList(),
    createdAt: String = "2026-09-05T14:22:00Z",
    answerText: String? = null,
    answeredAt: String? = null,
): InquiryDetail =
    InquiryDetail(
        inquiryId = inquiryId,
        category = category,
        status = if (answerText == null) InquiryStatus.RECEIVED else InquiryStatus.ANSWERED,
        body = body,
        imageUrls = imageUrls,
        createdAt = createdAt,
        answerText = answerText,
        answeredAt = answeredAt ?: answerText?.let { "2026-09-06T11:08:00Z" },
    )
