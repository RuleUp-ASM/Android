package com.ruleup.verification.domain.entity

/**
 * 인증 이의 제기 규칙 (인증 정책 §5 · 챌린지 정책 §7).
 *
 * **판정 단계가 없다.** 형식 요건만 확인하고 즉시 자동 인용하며, 남용은 판정이 아니라 이상탐지가
 * 막는다. 그래서 횟수 한도도, 기각 상태도, 재이의도 없다.
 */
object AppealPolicy {
    // 사유 최소 길이. 미달이면 접수 자체가 되지 않는다(400 INVALID_REASON).
    const val MIN_REASON_LENGTH = 10
}

/**
 * 이의 처리 트랙 (명세 appeals `track`).
 * - [A]: 전송 지연·권한 이력 등 **서버 데이터로 확인 가능한 사유** — 규칙 기반 자동 판정
 * - [B]: 그 외 전부 — 형식 요건만 보고 즉시 인용
 *
 * 어느 쪽이든 결과는 인용이다. 사용자에게는 트랙을 구분해 보여줄 이유가 없고, 운영 로그용이다.
 */
enum class AppealTrack(
    val value: String,
) {
    A("A"),
    B("B"),
    ;

    companion object {
        fun fromValue(value: String?): AppealTrack? = entries.find { it.value == value }
    }
}

/** 이의 인용으로 되돌려진 기록 (명세 appeals `restored`). 서버가 계산한 결과를 그대로 싣는다. */
data class AppealRestored(
    // 정정된 인증 상태(예: DONE)
    val verification: TodayResultStatus?,
    val streak: Int?,
    val scoreDelta: Int?,
)

/**
 * 이의 접수 결과 (명세 POST /verifications/{verificationId}/appeals).
 *
 * 응답의 `result` 는 `ACCEPTED` 고정이라 타입으로 옮기지 않았다 — 접수되면 인용이고, 요건
 * 미충족은 접수 자체가 되지 않아 예외로 온다. 상태 필드를 두면 있지도 않은 "대기 중"·"기각"을
 * 화면이 표현할 수 있게 된다.
 */
data class AppealReceipt(
    val appealId: String,
    val track: AppealTrack?,
    val restored: AppealRestored?,
)
