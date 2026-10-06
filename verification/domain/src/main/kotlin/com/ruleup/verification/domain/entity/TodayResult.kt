package com.ruleup.verification.domain.entity

/** 오늘 인증 상태. */
enum class TodayResultStatus(
    val value: String,
) {
    IN_PROGRESS("IN_PROGRESS"),
    FAIL_EXPECTED("FAIL_EXPECTED"),
    DONE("DONE"),
    FAILED("FAILED"),
    NOT_TARGET("NOT_TARGET"),
    ;

    /** 실패로 확정됐는가. */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        /** 미인식 값은 null 이다. */
        fun fromValue(value: String?): TodayResultStatus? = entries.find { it.value == value }
    }
}

/** 판정 불가 사유. */
enum class PendingReason {
    PERMISSION_MISSING,
    NO_SIGNAL,
    ;

    companion object {
        /** 모르는 사유는 null */
        fun fromValue(value: String?): PendingReason? = entries.find { it.name == value }
    }
}

/** 연속 성공 일수. */
data class VerificationStreak(
    val before: Int,
    val after: Int,
)

/** 아직 사용자가 확인하지 않은 판정. */
data class UnacknowledgedResult(
    val verificationId: String,
    val result: String,
)

/** 이의 제기 가능 여부. */
data class AppealChance(
    val eligibleUntil: String?,
    val eligible: Boolean,
)

/** 오늘 인증 결과. */
data class TodayResult(
    // 오늘 (KST)
    val date: String,
    // 이의 제기 대상 인증 건 ID.
    val verificationId: String?,
    val status: TodayResultStatus?,
    // 인증 창 표시 문구
    val window: String?,
    // 확정 시각
    val confirmedAt: String?,
    val failureReason: FailureReason?,
    val streak: VerificationStreak?,
    val unacknowledged: UnacknowledgedResult?,
    val appeal: AppealChance?,
    val pendingReason: PendingReason? = null,
    // 「체류 42분 / 목표 60분」처럼 그대로 보여줄 판정 근거 한 줄.
    val evidenceSummary: String? = null,
) {
    /**
     * 근거 한 줄의 「값 / 목표 값」에서 읽은 오늘 진행 비율(0~1).
     * 서버가 따로 숫자를 주지 않아 문구에서 읽는다. 목표가 없는 근거(기상 시각 등)나 목표가 0 이면 null — 막대를 그리지 않는다.
     */
    val evidenceProgress: Double?
        get() {
            val match = EVIDENCE_PROGRESS.find(evidenceSummary ?: return null) ?: return null
            val value = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null
            val goal =
                match.groupValues[2]
                    .replace(",", "")
                    .toDoubleOrNull()
                    ?.takeIf { it > 0 } ?: return null
            return (value / goal).coerceIn(0.0, 1.0)
        }

    private companion object {
        val EVIDENCE_PROGRESS = Regex("""(\d[\d,]*(?:\.\d+)?)[^/\d]*/\s*목표\s*(\d[\d,]*(?:\.\d+)?)""")
    }
}
