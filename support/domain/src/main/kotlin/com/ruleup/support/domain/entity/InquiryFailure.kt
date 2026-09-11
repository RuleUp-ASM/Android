package com.ruleup.support.domain.entity

/**
 * 문의에서 화면이 구분해야 하는 실패.
 *
 * data 가 서버 에러 코드를 여기로 옮기고 화면은 이 enum 만 본다 — 화면이 `core:network` 의
 * `ApiException` 과 코드 문자열에 묶이면 서버가 코드를 바꿀 때 화면을 전부 뒤져야 한다.
 *
 * **계정 잠금은 여기 없다.** 문의는 잠금 화이트리스트에 들어 있어 잠긴 계정도 201 을 받는다
 * (운영자 제재 정책 §5.3) — 제재 재검토가 이 채널로 들어오므로 막으면 다툴 방법이 사라진다.
 * 여기에 잠금 갈래를 두면 있지도 않은 차단을 화면이 표현하게 된다.
 */
enum class InquiryFailure {
    /** 오늘 접수 상한을 다 썼다(429 INQUIRY_DAILY_LIMIT). 내일 다시 가능하다. */
    DAILY_LIMIT,

    /** 본문이 길이 범위를 벗어났다(400 INQUIRY_BODY_LENGTH). [InquiryBody] 를 거치면 나오지 않는다. */
    BODY_LENGTH,

    /** 사진이 허용 장수를 넘었다(400 INQUIRY_IMAGE_LIMIT). [InquirySubmission] 을 거치면 나오지 않는다. */
    IMAGE_LIMIT,

    /** 사진 업로드가 형식·크기에서 막혔다(413·415·400). 다른 사진을 고르면 풀린다. */
    IMAGE_REJECTED,

    /** 없거나 **내 문의가 아니다**(404 INQUIRY_NOT_FOUND). 서버가 둘을 구분해 주지 않는다. */
    NOT_FOUND,

    /** 네트워크·오프라인. 재시도로 풀릴 수 있다. */
    NETWORK,

    UNKNOWN,
}

/** [InquiryFailure] 를 실은 예외. 화면은 [failure] 로 분기하고 [message] 는 안내 문구로 쓴다. */
class InquiryException(
    val failure: InquiryFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
