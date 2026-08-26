package com.ruleup.verification.domain.entity

/**
 * 오늘 인증 상태 (명세: GET /challenges/{id}/verifications/today `status`).
 *
 * 판정 이전 구간이 둘로 나뉘는 게 핵심이다 — [IN_PROGRESS] 는 인증 창이 아직 열려 있는 상태이고,
 * [CHECKING] 은 귀속일은 끝났지만 아직 확정 전 — 늦게 도착하는 신호를 계속 받는 유예 구간이다.
 * 둘 다 **실패가 아니다** — 검사중은 성공·실패 양쪽으로 열려 있다.
 */
enum class TodayResultStatus(
    val value: String,
) {
    IN_PROGRESS("IN_PROGRESS"),
    CHECKING("CHECKING"),
    DONE("DONE"),
    FAILED("FAILED"),
    NOT_TARGET("NOT_TARGET"),
    ;

    /** 실패로 확정됐는가 — 이의 제기 안내의 조건. 재평가 중([CHECKING])은 아직 실패가 아니다. */
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
 * 이의 제기 가능 여부 (명세 `appeal`). `FAILED` 일 때만 내려온다.
 *
 * [eligibleUntil] 은 **실패 확정 후 1일**이다(인증 정책 §5.2 · 챌린지 정책 §7.2). 횟수 한도는
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
)
