package com.ruleup.challenge.domain.entity

/**
 * 대기열 수용 한도.
 *
 * 정원이 찬 방에서 참여를 누르면 대기열에 들어가지만, **무한정 받지 않는다** — 대기 인원이
 * 정원의 50% 를 넘으면 그 방은 사실상 못 들어가는 방이라 기다리게 두는 것 자체가 기만이다.
 * 화면은 이 한도를 넘은 순간 "대기하기" 버튼을 **비활성**으로 그린다(눌러서 실패하게 두지 않는다).
 *
 * 정수 나눗셈이라 결과는 **내림**이다 — "50% 까지만"을 넘지 않는 쪽으로 해석했다.
 * 정원 3 → 1명, 정원 5 → 2명, 정원 10 → 5명이며 **정원 1 인 방은 대기열이 없다(0명)**.
 * (반올림/올림이 정책이면 여기 한 곳만 고치면 된다 — 화면과 서버가 같은 수를 봐야 하므로
 * 이 값을 화면에서 다시 계산하지 않는다.)
 */
object WaitlistPolicy {
    // 정원 대비 대기 허용 비율(%)
    const val CAPACITY_PERCENT = 50

    /** [capacity] 정원의 방이 받을 수 있는 최대 대기 인원. */
    fun maxSize(capacity: Int): Int = capacity.coerceAtLeast(0) * CAPACITY_PERCENT / 100
}

/**
 * 내 대기 순번.
 *
 * 서버가 순번을 **아직 계산하지 못한 상태**([Calculating])와 순번이 확정된 상태([Assigned])는 다른
 * 사실이다. 서버 값이 null 인데 이를 0 이나 1 로 접으면 "곧 들어갈 수 있다"는 없는 약속이 화면에
 * 생긴다 — 그래서 숫자가 아니라 타입으로 갈라 두고, 화면은 [Calculating] 을 "확인 중"으로 그린다.
 *
 * 순번은 1 부터다. 서버가 0 이하를 보내면 그것도 순번이 아니므로 [fromValue] 가 [Calculating] 으로
 * 떨어뜨린다 — 모르는 것을 "1번"으로 보여주는 것보다 모른다고 말하는 편이 정확하다.
 */
sealed interface WaitlistPosition {
    /** 순번 계산 전(서버 null). 화면은 숫자 대신 "확인 중"을 그린다. */
    data object Calculating : WaitlistPosition

    /** 확정된 순번. 1 이면 다음 자리의 주인이다. */
    data class Assigned(
        val order: Int,
    ) : WaitlistPosition {
        init {
            require(order >= 1) { "대기 순번은 1부터입니다: $order" }
        }
    }

    companion object {
        /** 서버 응답값(null 가능)을 순번으로 옮긴다. null·0 이하는 모두 [Calculating] 이다. */
        fun fromValue(value: Int?): WaitlistPosition =
            if (value == null || value < 1) Calculating else Assigned(value)
    }
}

/**
 * 내 대기열 등록 상태.
 *
 * [waitingCount] 는 나를 포함한 현재 대기 인원이다. 자리가 나면 **선착순으로 서버가 자동 참여**시키므로
 * 앱이 승격을 요청하는 경로는 없다 — 앱은 상태를 다시 받아 확인만 한다.
 */
data class WaitlistEntry(
    val challengeId: String,
    val position: WaitlistPosition,
    val waitingCount: Int,
    // ISO datetime, 대기열에 들어온 시각(선착순의 기준)
    val joinedAt: String,
)

/**
 * 방 하나의 대기열 현황. 정원이 찬 방의 참여 버튼을 무엇으로 그릴지가 전부 여기서 결정된다.
 *
 * [myEntry] 가 null 이면 나는 대기 중이 아니다.
 */
data class WaitlistStatus(
    val capacity: Int,
    // 현재 대기 인원
    val waitingCount: Int,
    val myEntry: WaitlistEntry?,
) {
    /** 이 방이 받을 수 있는 최대 대기 인원. */
    val maxSize: Int
        get() = WaitlistPolicy.maxSize(capacity)

    /** 대기열까지 꽉 찼는가 — 버튼을 아예 막아야 하는 상태다. */
    val isFull: Boolean
        get() = waitingCount >= maxSize

    /** 지금 대기열에 들어갈 수 있는가. 이미 대기 중이면 다시 넣지 않는다. */
    val canEnqueue: Boolean
        get() = myEntry == null && !isFull

    /** 대기 중인가 — 참여 버튼 자리에 "대기 중 · 내 순번" 을 그린다. */
    val isWaiting: Boolean
        get() = myEntry != null
}

/**
 * 대기가 끝난 이유.
 *
 * [PROMOTED] 는 자리가 나 **자동으로 참여된 것**이라 실패가 아니다 — 화면도 오류가 아니라 입장 안내로
 * 다룬다. [CHALLENGE_STARTED] 는 방이 시작돼 대기열이 통째로 사라진 경우로, 개인의 잘못이 아니라
 * 방의 상태 변화다.
 */
enum class WaitlistExitReason(
    val value: String,
) {
    // 자리가 나 선착순으로 자동 참여됨
    PROMOTED("PROMOTED"),

    // 대기 중 방이 시작됨 — 대기열 전체 소멸
    CHALLENGE_STARTED("CHALLENGE_STARTED"),

    // 본인이 대기를 취소함
    CANCELED("CANCELED"),
    ;

    /** 자동 참여로 끝났는가 — 이 경우만 방으로 이동시킨다. */
    val isPromoted: Boolean
        get() = this == PROMOTED

    companion object {
        fun fromValue(value: String?): WaitlistExitReason? = entries.find { it.value == value }
    }
}

/**
 * 참여료 환불 결과.
 *
 * **"참여료가 없던 방"과 "환불액 0원"은 다른 사실**이라 하나의 숫자로 접지 않는다 — 접으면 화면이
 * 참여료 없는 방에도 "0원 환불" 같은 문구를 띄운다. [NotCharged] 면 환불 안내 자체를 그리지 않는다.
 */
sealed interface WaitlistRefund {
    /** 참여료가 없던 방 — 환불할 것이 없다. */
    data object NotCharged : WaitlistRefund

    /** 참여료가 환불됐다. */
    data class Refunded(
        val amount: Int,
    ) : WaitlistRefund {
        init {
            require(amount >= 0) { "환불액은 음수일 수 없습니다: $amount" }
        }
    }
}

/**
 * 대기 종료 통지.
 *
 * 대기 중 방이 시작되면 대기열은 **통째로** 사라지고, 참여료를 냈다면 환불된다
 * ([reason] = [WaitlistExitReason.CHALLENGE_STARTED]). 승격([WaitlistExitReason.PROMOTED])은
 * 참여료가 실제 참여에 쓰였으므로 [refund] 가 [WaitlistRefund.NotCharged] 다.
 */
data class WaitlistExit(
    val challengeId: String,
    val reason: WaitlistExitReason,
    val refund: WaitlistRefund,
)

/**
 * 대기열까지 가득 찼다.
 *
 * 화면은 버튼을 미리 막으므로 정상 경로에서는 나오지 않지만, 상세를 띄워 둔 사이 다른 사람이 먼저
 * 들어오면 발생한다 — 현황을 다시 받아 버튼을 다시 그린다.
 */
class WaitlistFullException : Exception("대기 인원이 모두 찼어요.")

/**
 * 방이 시작돼 대기열이 사라졌다. 참여료가 있었으면 [refund] 로 환불 결과가 함께 온다.
 * 실패 문구가 아니라 "이 방은 이미 시작됐어요" 안내로 다룬다.
 */
class WaitlistClosedException(
    val refund: WaitlistRefund = WaitlistRefund.NotCharged,
) : Exception("이미 시작된 챌린지예요.")
