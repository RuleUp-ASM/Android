package com.ruleup.support.domain.entity

/** 문의에서 화면이 구분해야 하는 실패. */
enum class InquiryFailure {
    /** 오늘 접수 상한을 다 썼다(429 INQUIRY_DAILY_LIMIT). */
    DAILY_LIMIT,

    /** 본문이 길이 범위를 벗어났다(400 INQUIRY_BODY_LENGTH). */
    BODY_LENGTH,

    /** 사진이 허용 장수를 넘었다(400 INQUIRY_IMAGE_LIMIT). */
    IMAGE_LIMIT,

    /** 사진 업로드가 형식·크기에서 막혔다(413·415·400). */
    IMAGE_REJECTED,

    /** 없거나 내 문의가 아니다(404 INQUIRY_NOT_FOUND). */
    NOT_FOUND,

    /** 네트워크·오프라인. */
    NETWORK,

    UNKNOWN,
}

/** [InquiryFailure] 를 실은 예외. */
class InquiryException(
    val failure: InquiryFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
