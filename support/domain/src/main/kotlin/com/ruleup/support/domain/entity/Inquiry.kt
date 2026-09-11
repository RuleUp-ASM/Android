package com.ruleup.support.domain.entity

/**
 * 문의 입력의 허용 범위 (명세 POST /api/v1/inquiries).
 *
 * 화면 위젯과 [InquiryBody] 검증이 **같은 값**을 본다 — 숫자가 화면마다 따로 살면 한쪽만 고쳐져도
 * 아무도 모른다.
 */
object InquiryLimits {
    // 벗어나면 서버가 400 INQUIRY_BODY_LENGTH 로 막는다
    const val BODY_MIN_LENGTH = 10
    const val BODY_MAX_LENGTH = 1_000

    // 초과하면 400 INQUIRY_IMAGE_LIMIT
    const val IMAGE_MAX_COUNT = 3

    // KST 기준 하루 접수 상한. 소진하면 429 INQUIRY_DAILY_LIMIT
    const val DAILY_LIMIT = 3
}

/**
 * 문의 분류 (명세 `category`). **접수 시 1개만** 고른다.
 *
 * 운영자가 접수 후 분류를 바꿀 수 있지만 **바뀐 사실은 내려오지 않는다** — 응답의 분류는 언제나
 * "현재 분류"이고 원본 분류 필드는 계약에 없다. 화면이 "내가 고른 것과 다르다"를 표현할 방법도,
 * 표현할 이유도 없다.
 */
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
        /**
         * 미지 값은 null — 분류 칩만 비우고 본문·상태는 그대로 그린다. 운영자가 6종 밖으로 옮겼거나
         * 서버가 분류를 늘렸을 때, 목록에서 그 문의를 통째로 숨기면 사용자가 자기 문의를 잃는다.
         */
        fun fromValue(value: String?): InquiryCategory? = entries.find { it.value == value }
    }
}

/**
 * 문의 처리 상태 (명세 `status`). **2종뿐이다.**
 *
 * 그 사이 「검토중」 같은 중간 상태를 두지 않는다 — 전이만 늘고 사용자에게 주는 정보가 없다.
 * [ANSWERED] 가 곧 종결이라 그 뒤로 상태가 바뀌지 않는다.
 */
enum class InquiryStatus(
    val value: String,
    val label: String,
) {
    RECEIVED("RECEIVED", "접수됨"),
    ANSWERED("ANSWERED", "답변 완료"),
    ;

    companion object {
        /**
         * 미지 값은 [RECEIVED] 로 본다 — 답변이 없는데 「답변 완료」로 보이면 사용자가 답변을 찾아
         * 상세로 들어갔다가 빈 화면을 만난다. 반대 방향의 오류가 덜 나쁘다.
         */
        fun fromValue(value: String?): InquiryStatus = entries.find { it.value == value } ?: RECEIVED
    }
}

/**
 * 문의 본문. 길이 규칙을 값에 가둬 화면을 거치지 않는 경로에서도 같은 규칙이 걸리게 한다.
 *
 * 화면도 같은 상수로 입력을 막지만 그건 UX 이지 정합성이 아니다 — 상태 복원처럼 화면을 거치지 않는
 * 경로가 남는다. 서버의 400 `INQUIRY_BODY_LENGTH` 를 앱에서 먼저 잡는 자리다.
 */
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

        /** 입력 중 버튼 활성 판정용. [of] 를 try/catch 로 감싸 쓰지 않게 하려고 따로 둔다. */
        fun isValid(raw: String): Boolean = raw.trim().length in InquiryLimits.BODY_MIN_LENGTH..InquiryLimits.BODY_MAX_LENGTH
    }
}

/**
 * 접수 요청 (명세 POST /api/v1/inquiries request).
 *
 * 자동 첨부 4종(앱 버전 · OS 버전 · 기기 모델 · 오류 로그 ID)은 여기 없다 — 사용자가 고르는 값이
 * 아니라 **토글 없이 붙는 진단 정보**라, data 가 [com.ruleup.support.domain.repository.DeviceContextProvider]
 * 로 채워 보낸다. 화면은 그 사실을 고지만 한다.
 */
data class InquirySubmission(
    val category: InquiryCategory,
    val body: InquiryBody,
    // 업로드 API 가 발급한 주소만 실린다. 빈 목록이면 사진 없이 접수된다.
    val imageUrls: List<String> = emptyList(),
) {
    init {
        require(imageUrls.size <= InquiryLimits.IMAGE_MAX_COUNT) {
            "사진은 ${InquiryLimits.IMAGE_MAX_COUNT}장까지 첨부할 수 있어요."
        }
    }
}

/**
 * 접수 결과 (명세 POST /api/v1/inquiries 201).
 *
 * [inquiryId] 가 곧 **접수번호**다. 서버가 UUID 를 주고 화면은 그대로 노출한다 — CS 담당자가 이
 * 값으로 조회하므로 앱이 따로 보기 좋은 번호를 만들면 대조 키로 쓸 수 없게 된다.
 */
data class InquiryReceipt(
    val inquiryId: String,
    val status: InquiryStatus,
    // ISO-8601
    val createdAt: String,
)

/**
 * 목록 항목 1건 (명세 GET /api/v1/inquiries `items[]`).
 *
 * [answeredAt] 이 채워졌는지가 **새 답변 배지**의 기준이다(명세). 읽음 지점을 서버가 보관하지
 * 않으므로 한 번 본 답변도 계속 새 답변으로 남는다 — 읽음 계약이 생기면 그때 기준을 옮긴다.
 */
data class InquirySummary(
    val inquiryId: String,
    val category: InquiryCategory?,
    val status: InquiryStatus,
    // 본문 앞부분 60자 + "…". 전문은 상세에서만 온다
    val preview: String,
    // ISO-8601
    val createdAt: String,
    // ISO-8601. 미답변이면 null
    val answeredAt: String?,
) {
    val hasNewAnswer: Boolean
        get() = answeredAt != null
}

/**
 * 문의 상세 (명세 GET /api/v1/inquiries/{inquiryId}).
 *
 * **열람 전용이다.** 답변 뒤 같은 스레드에 글을 더하는 경로가 없어 입력바를 두지 않는다 — 같은
 * 사안이라도 다시 물으려면 새 문의로 접수하고, 새 문의에도 같은 응답 기한이 적용된다.
 */
data class InquiryDetail(
    val inquiryId: String,
    val category: InquiryCategory?,
    val status: InquiryStatus,
    val body: String,
    val imageUrls: List<String>,
    // ISO-8601
    val createdAt: String,
    // 운영팀 답변 전문. 미답변이면 null
    val answerText: String?,
    // ISO-8601. 미답변이면 null
    val answeredAt: String?,
)

/**
 * 접수에 자동으로 붙는 진단 정보 (명세의 `appVersion`·`osVersion`·`deviceModel`·`errorLogId`).
 *
 * **값이 없으면 없는 대로 보낸다** — 문의를 막을 값이 아니다. 그래서 전부 nullable 이고, data 가
 * 채우지 못한 항목은 요청에서 빠진다.
 */
data class InquiryDeviceContext(
    val appVersion: String?,
    val osVersion: String?,
    val deviceModel: String?,
    val errorLogId: String?,
)
