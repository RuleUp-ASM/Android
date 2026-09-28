package com.ruleup.report.domain.entity

/** 신고·차단에서 화면이 구분해야 하는 실패. */
enum class ReportFailure {
    /** 운영자가 남용으로 확정해 신고 기능이 정지됐다(403 REPORT_SUSPENDED). */
    SUSPENDED,

    /** 본인은 신고할 수 없다(400). */
    SELF_TARGET,

    /** 대상 지정이 잘못됐다(400) */
    INVALID_TARGET,

    /** 대상에 없는 사유를 골랐다(400). */
    INVALID_REASON,

    /** 신고하려는 사용자·챌린지가 이미 없다(404). */
    TARGET_NOT_FOUND,

    /** 계정 잠금 중 막히는 기능(403). */
    ACCOUNT_LOCKED,

    /** 이미 해제됐거나 애초에 차단 목록에 없다(404). */
    BLOCK_ENTRY_NOT_FOUND,

    /** 네트워크·오프라인. */
    NETWORK,

    ALREADY_REPORTED,

    UNKNOWN,
}

/** [ReportFailure] 를 실은 예외. */
class ReportException(
    val failure: ReportFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
