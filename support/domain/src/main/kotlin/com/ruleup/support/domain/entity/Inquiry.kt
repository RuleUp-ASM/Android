package com.ruleup.support.domain.entity

/** 문의 입력의 허용 범위. */
object InquiryLimits {
    // 벗어나면 서버가 400 INQUIRY_BODY_LENGTH 로 막는다
    const val BODY_MIN_LENGTH = 10
    const val BODY_MAX_LENGTH = 1_000

    // 초과하면 400 INQUIRY_IMAGE_LIMIT
    const val IMAGE_MAX_COUNT = 3

    // KST 기준 하루 접수 상한.
    const val DAILY_LIMIT = 3
}

/** 문의 분류. */
enum class InquiryCategory(
    val value: String,
    val title: String,
    val description: String,
) {
    VERIFICATION(
        value = "VERIFICATION",
        title = "인증 · 판정",
        description = "자동 인증이 안 되거나 판정 결과가 이상해요",
    ),
    DEVICE_PERMISSION(
        value = "DEVICE_PERMISSION",
        title = "권한 · 기기 연동",
        description = "위치 · 헬스 커넥트 · 스크린타임 권한 문제",
    ),
    CHALLENGE_GROUP(
        value = "CHALLENGE_GROUP",
        title = "챌린지 · 그룹",
        description = "생성 · 참여 · 방 운영에 문제가 있어요",
    ),
    ACCOUNT_LOGIN(
        value = "ACCOUNT_LOGIN",
        title = "계정 · 로그인",
        description = "소셜 로그인 · 닉네임 · 탈퇴 · 복구",
    ),
    REPORT_SANCTION(
        value = "REPORT_SANCTION",
        title = "신고 · 제재",
        description = "신고 결과 문의 · 제재 재검토 요청",
    ),
    ERROR_ETC(
        value = "ERROR_ETC",
        title = "오류 · 제안 · 기타",
        description = "앱 오류 · 기능 제안 · 그 밖의 문의",
    ),
    ;

    companion object {
        /** 미지 값은 null */
        fun fromValue(value: String?): InquiryCategory? = entries.find { it.value == value }
    }
}

/** 문의 처리 상태. */
enum class InquiryStatus(
    val value: String,
    val label: String,
) {
    RECEIVED("RECEIVED", "접수됨"),
    ANSWERED("ANSWERED", "답변 완료"),
    ;

    companion object {
        /** 미지 값은 [RECEIVED] 로 본다 */
        fun fromValue(value: String?): InquiryStatus = entries.find { it.value == value } ?: RECEIVED
    }
}

/** 문의 본문. */
@JvmInline
value class InquiryBody private constructor(
    val value: String,
) {
    companion object {
        fun of(raw: String): InquiryBody {
            val trimmed = raw.trim()
            require(trimmed.length >= InquiryLimits.BODY_MIN_LENGTH) {
                "문의 내용을 ${InquiryLimits.BODY_MIN_LENGTH}자 이상 적어 주세요."
            }
            require(trimmed.length <= InquiryLimits.BODY_MAX_LENGTH) {
                "문의 내용은 ${InquiryLimits.BODY_MAX_LENGTH}자까지예요."
            }
            return InquiryBody(trimmed)
        }

        /** 입력 중 버튼 활성 판정용. */
        fun isValid(raw: String): Boolean = raw.trim().length in InquiryLimits.BODY_MIN_LENGTH..InquiryLimits.BODY_MAX_LENGTH
    }
}

/** 접수 요청. */
data class InquirySubmission(
    val category: InquiryCategory,
    val body: InquiryBody,
    // 업로드 API 가 발급한 주소만 실린다.
    val imageUrls: List<String> = emptyList(),
) {
    init {
        require(imageUrls.size <= InquiryLimits.IMAGE_MAX_COUNT) {
            "사진은 ${InquiryLimits.IMAGE_MAX_COUNT}장까지 첨부할 수 있어요."
        }
    }
}

/** 접수 결과. */
data class InquiryReceipt(
    val inquiryId: String,
    val status: InquiryStatus,
    // ISO-8601
    val createdAt: String,
)

/** 목록 항목 1건. */
data class InquirySummary(
    val inquiryId: String,
    val category: InquiryCategory?,
    val status: InquiryStatus,
    // 본문 앞부분 60자 + "…".
    val preview: String,
    // ISO-8601
    val createdAt: String,
    // ISO-8601.
    val answeredAt: String?,
) {
    val hasNewAnswer: Boolean
        get() = answeredAt != null
}

/** 문의 상세. */
data class InquiryDetail(
    val inquiryId: String,
    val category: InquiryCategory?,
    val status: InquiryStatus,
    val body: String,
    val imageUrls: List<String>,
    // ISO-8601
    val createdAt: String,
    // 운영팀 답변 전문.
    val answerText: String?,
    // ISO-8601.
    val answeredAt: String?,
)

/** 접수에 자동으로 붙는 진단 정보. */
data class InquiryDeviceContext(
    val appVersion: String?,
    val osVersion: String?,
    val deviceModel: String?,
    val errorLogId: String?,
)
