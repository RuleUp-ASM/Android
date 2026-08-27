package com.ruleup.challenge.domain.entity

/**
 * 대기열 상한 정책 (정원의 50%).
 *
 * 최종 판정은 서버다 — 이 계산은 **눌러보게 두지 않기 위한** 사전 차단이고, 서버가 내려준
 * [ChallengeWaitlist.maxSize] 가 있으면 그쪽이 우선이다. 두 값이 어긋나도 등록은 서버가 409 로 막는다.
 */
object WaitlistPolicy {
    const val CAPACITY_RATIO = 0.5

    /**
     * 내림한다 — 정원 7명이면 3명까지. 정원 1명 방은 상한이 0 이라 대기열이 아예 열리지 않는다.
     * (반올림·올림 여부는 서버 명세 확인 대상이다.)
     */
    fun maxSize(capacity: Int): Int = (capacity * CAPACITY_RATIO).toInt()
}

/**
 * 내 대기 순번 (명세 `waitlist.myPosition`).
 *
 * 서버가 순번을 아직 확정하지 못하면 값이 비어 내려온다. **비었다고 1번으로 접으면 안 된다** —
 * "다음 차례" 로 읽고 기다린 사람이 자리를 놓치면 그건 앱이 만든 거짓말이다. 그래서 순번을 `Int?` 로
 * 들고 다니지 않고 이 타입으로 받는다: [Calculating] 을 받은 화면에는 "확인 중" 말고 그릴 게 없다.
 */
sealed interface WaitlistPosition {
    /** 서버가 순번을 계산하는 중. */
    data object Calculating : WaitlistPosition

    data class Assigned(
        val number: Int,
    ) : WaitlistPosition {
        init {
            require(number >= 1) { "대기 순번은 1부터입니다: $number" }
        }
    }

    companion object {
        /**
         * 서버 값을 순번으로 바꾼다. **0 이하도 [Calculating]** 이다 — 계산 전과 구분할 근거가 없는데
         * 1번으로 올려 그리면 위의 거짓말이 그대로 돌아온다.
         */
        fun of(raw: Int?): WaitlistPosition = if (raw == null || raw < 1) Calculating else Assigned(raw)
    }
}

/**
 * 정원이 찬 방의 대기열 현황 (명세 `waitlist`). 대기열이 없는 방에서는 블록 자체가 내려오지 않는다.
 *
 * **사라지는 게 정상이다** — 대기 중에 방이 시작되면 서버가 대기열을 통째로 비우므로 이 블록은 null 이
 * 되고, 대기하던 사람은 멤버가 되지 못한 채 남는다(참여료가 있었으면 환불도 서버가 정산한다).
 * 화면은 이 null 을 실패로 다루지 않고 대기 표시를 지운다.
 */
data class ChallengeWaitlist(
    val waitingCount: Int,
    // 서버가 계산한 상한. 앱 쪽 계산은 [WaitlistPolicy.maxSize]
    val maxSize: Int,
    // 대기 중이 아니면 null
    val myPosition: WaitlistPosition?,
) {
    /** 더 받을 자리가 없는가 — 참여 버튼 **자체**를 막는 조건이다. */
    val isFull: Boolean
        get() = waitingCount >= maxSize

    val isWaiting: Boolean
        get() = myPosition != null
}

/**
 * 참여 CTA 가 실제로 하는 일. 계산은 [ChallengeDetail.joinAction] 한 곳에만 둔다 —
 * 화면마다 `isFull`·`joinBlockReason`·`waitlist` 를 따로 조합하면 카드와 상세가 다른 문구를 낸다.
 */
enum class JoinAction {
    JOIN,
    JOIN_WAITLIST,
    BLOCKED,
}

/**
 * 대기 등록 결과 (명세 POST /challenges/{challengeId}/waitlist).
 *
 * [requiredPermissions] 는 참고용이다 — 자리가 나면 **묻지 않고 자동 참여**되므로 자동 인증 방의 권한은
 * 대기 등록 전에 확보돼 있어야 한다. 참여가 일어나는 순간에는 권한 화면을 띄울 기회가 없다.
 */
data class WaitlistTicket(
    val position: WaitlistPosition,
    val waitingCount: Int,
    val requiredPermissions: List<String>,
)
