package com.ruleup.support.domain.fake

import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryReceipt
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.entity.InquirySubmission
import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.support.domain.repository.InquiryRepository

/**
 * 테스트용 [InquiryRepository]. 준비하지 않은 메서드는 호출되면 실패한다 — 화면이 의도치 않은
 * 조회를 해도 조용히 지나가지 않게 하려는 것이다.
 *
 * 목록만 기본값을 둔다. **설정 허브가 새 답변 뱃지 때문에 이 조회를 부수적으로 부르므로**,
 * 준비하지 않았다고 설정 테스트가 줄줄이 깨지면 그 화면의 진짜 계약이 가려진다.
 */
class FakeInquiryRepository(
    private val inquiries: (() -> List<InquirySummary>)? = null,
    private val detail: ((String) -> InquiryDetail)? = null,
    private val submit: ((InquirySubmission) -> InquiryReceipt)? = null,
    private val upload: ((String) -> String)? = null,
) : InquiryRepository {
    val calls = mutableListOf<String>()

    /** 접수에 실제로 실려 간 값. 분류·본문·사진 주소가 화면과 맞는지 보는 자리다. */
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

/** 목록 픽스처. 테스트 본문에는 그 테스트가 신경 쓰는 값만 준다. */
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

/** 상세 픽스처. [answerText] 를 주면 답변 완료 상태가 된다. */
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
