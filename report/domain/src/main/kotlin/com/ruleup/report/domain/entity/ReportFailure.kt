package com.ruleup.report.domain.entity

/**
 * 신고·차단에서 화면이 구분해야 하는 실패.
 *
 * data 가 서버 에러 코드를 여기로 옮기고 화면은 이 enum 만 본다 — 화면이 `core:network` 의
 * `ApiException` 과 코드 문자열에 묶이면 서버가 코드를 바꿀 때 화면을 전부 뒤져야 한다.
 */
enum class ReportFailure {
    /**
     * 운영자가 남용으로 확정해 신고 기능이 정지됐다(403 REPORT_SUSPENDED).
     * 재시도로 풀리지 않으므로 화면은 다시 시도를 권하지 않는다.
     */
    SUSPENDED,

    /** 본인은 신고할 수 없다(400). */
    SELF_TARGET,

    /**
     * 대상 지정이 잘못됐다(400) — id 형식 오류이거나, 프로필 밖 사용자 신고인데 챌린지가 빠졌다.
     * 후자는 [ReportTarget.User] 가 이미 막으므로 여기까지 오면 앱 버그다.
     */
    INVALID_TARGET,

    /** 대상에 없는 사유를 골랐다(400). [ReportReason.forChallenge] 를 안 거친 경로다. */
    INVALID_REASON,

    /** 신고하려는 사용자·챌린지가 이미 없다(404). */
    TARGET_NOT_FOUND,

    /** 계정 잠금 중 막히는 기능(403). 둘러보기만 가능한 상태다. */
    ACCOUNT_LOCKED,

    /** 이미 해제됐거나 애초에 차단 목록에 없다(404). 차단 해제에서만 나온다. */
    BLOCK_ENTRY_NOT_FOUND,

    /** 네트워크·오프라인. 재시도로 풀릴 수 있다. */
    NETWORK,

    UNKNOWN,
}

/** [ReportFailure] 를 실은 예외. 화면은 [failure] 로 분기하고 [message] 는 진단용으로만 쓴다. */
class ReportException(
    val failure: ReportFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
