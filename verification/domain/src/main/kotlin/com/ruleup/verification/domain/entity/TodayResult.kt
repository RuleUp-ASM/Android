package com.ruleup.verification.domain.entity

/**
 * 오늘 인증 상태 (명세: GET /challenges/{id}/verifications/today `status`).
 *
 * [FAIL_EXPECTED] 는 위반이 잡혔거나 귀속일이 끝났는데 목표 미달인 상태다. **확정 실패가 아니고
 * 실제 이의 신청 창이 여기다** — 확정([FAILED])되면 이의는 닫힌다. 구 `CHECKING` 은 폐기됐다.
 */
enum class TodayResultStatus(
    val value: String,
) {
    IN_PROGRESS("IN_PROGRESS"),
    FAIL_EXPECTED("FAIL_EXPECTED"),
    DONE("DONE"),
    FAILED("FAILED"),
    NOT_TARGET("NOT_TARGET"),
    ;

    /** 실패로 확정됐는가. 실패 예정([FAIL_EXPECTED])은 늦은 신호로 뒤집힐 수 있어 아직 실패가 아니다. */
    val isFailure: Boolean
        get() = this == FAILED

    companion object {
        /**
         * 미인식 값은 null 이다. 모르는 상태를 성공·실패 어느 쪽으로도 접지 않고 화면이 표기를
         * 생략하게 한다 — 잘못 접으면 성공한 날이 실패로 보이거나 그 반대가 된다.
         */
        fun fromValue(value: String?): TodayResultStatus? = entries.find { it.value == value }
    }
}

/**
 * 판정 불가 사유 (명세 `pendingReason`). 목표 미달은 여기 오지 않는다 — 그건 [FailureReason] 이다.
 * 할 일이 「권한 켜기」와 「더 하기」로 갈려서 층을 나눈다.
 */
enum class PendingReason {
    PERMISSION_MISSING,
    NO_SIGNAL,
    ;

    companion object {
        /** 모르는 사유는 null — 권한 문제로 접으면 멀쩡한 사용자를 권한 화면으로 보낸다. */
        fun fromValue(value: String?): PendingReason? = entries.find { it.name == value }
    }
}

/** 연속 성공 일수 (명세 `streak`). 판정 전후를 함께 줘 "끊겼다"를 화면이 표현할 수 있다. */
data class VerificationStreak(
    val before: Int,
    val after: Int,
)

/**
 * 아직 사용자가 확인하지 않은 판정 (명세 `unacknowledgedResult`).
 * 값이 있으면 결과 모달을 띄우고 ack 를 호출한다.
 */
data class UnacknowledgedResult(
    val verificationId: String,
    val result: String,
)

/**
 * 이의 제기 가능 여부 (명세 `appeal`). `FAILED`·`FAIL_EXPECTED` 에서 내려온다 — 실제로 낼 수 있는지는
 * 상태가 아니라 [eligible] 이 말한다.
 *
 * [eligibleUntil] 은 실패 확정과 같은 시각(귀속일 이틀 뒤 00:00 KST)인 **경계**다. 횟수 한도는
 * 없어졌으므로 "몇 회 남음"을 세지 않는다 — 남용은 이상탐지가 잡는다.
 */
data class AppealChance(
    val eligibleUntil: String?,
    val eligible: Boolean,
)

/**
 * 오늘 인증 결과 (명세: GET /challenges/{id}/verifications/today).
 *
 * 방 상세의 "오늘 내 인증" 카드와 판정 결과 모달이 같은 응답을 쓴다. 구
 * `GET /challenges/{id}/verification`(잠정 실패 버전)을 대체하는 계약이다.
 */
data class TodayResult(
    // 오늘 (KST)
    val date: String,
    // 이의 제기 대상 인증 건 ID. 이 값이 없으면 이의를 접수할 경로가 없어 진입점을 열지 않는다.
    val verificationId: String?,
    val status: TodayResultStatus?,
    // 인증 창 표시 문구 — 자동은 시간대("06:00-07:00"), 수동은 "자정 마감"
    val window: String?,
    // 확정 시각 — 성공은 조건 충족 즉시, 실패는 귀속일 이틀 뒤 00:00 KST 에 확정된다
    val confirmedAt: String?,
    val failureReason: FailureReason?,
    val streak: VerificationStreak?,
    val unacknowledged: UnacknowledgedResult?,
    val appeal: AppealChance?,
    val pendingReason: PendingReason? = null,
    // 「체류 42분 / 목표 60분」처럼 그대로 보여줄 판정 근거 한 줄. FAILED·FAIL_EXPECTED 에서만
    val evidenceSummary: String? = null,
)
