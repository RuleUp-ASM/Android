package com.ruleup.challenge.domain.entity

/** 대기열 상한. 화면의 잠금 조건과 [ChallengeWaitlist] 판정이 **같은 값**을 본다. */
object WaitlistPolicy {
    const val CAPACITY_RATIO = 0.5

    /** 내림한다 — 서버 반올림 규칙이 명세에 없어(정원 5 → 2? 3?) 좁은 쪽을 고른다. */
    fun maxWaiting(capacity: Int): Int = (capacity * CAPACITY_RATIO).toInt()
}

/** 내 대기 순번 (명세 `position`). 서버 계산이 끝나기 전에는 [Calculating] 이다. */
sealed interface WaitlistPosition {
    /** 1번으로 접으면 곧 입장한다고 믿고 기다린다 — 화면은 "확인 중"으로 표시한다. */
    data object Calculating : WaitlistPosition

    data class Of(
        val rank: Int,
    ) : WaitlistPosition {
        init {
            require(rank >= 1) { "대기 순번은 1부터입니다: $rank" }
        }
    }

    companion object {
        fun of(rank: Int?): WaitlistPosition = rank?.let(::Of) ?: Calculating
    }
}

/**
 * 방 대기열 현황 (명세 미확정 — `GET /challenges/{id}/waitlist` 로 가정).
 *
 * 자리가 나면 **서버가 선착순으로 자동 참여**시킨다. 앱이 폴링하거나 가입을 재시도하지 않는다.
 */
data class ChallengeWaitlist(
    val waitingCount: Int,
    val capacity: Int,
    // 줄 서 있지 않으면 null. 줄은 섰는데 순번만 미정인 건 [WaitlistPosition.Calculating] 이다.
    val myPosition: WaitlistPosition?,
) {
    val maxWaiting: Int
        get() = WaitlistPolicy.maxWaiting(capacity)

    /** 대기 상한을 채웠는가 — 참여 버튼 자체가 잠긴다. */
    val isFull: Boolean
        get() = waitingCount >= maxWaiting

    val isWaiting: Boolean
        get() = myPosition != null

    /** 지금 줄을 설 수 있는가. */
    val canEnter: Boolean
        get() = !isWaiting && !isFull
}

/** 참여 버튼이 실제로 하는 일. 정원이 차면 참여 대신 대기열 등록이 된다. */
sealed interface JoinAction {
    data object Join : JoinAction

    data object EnterWaitlist : JoinAction

    /** 대기 상한까지 찼다 — 대기열 안내가 아니라 버튼을 잠근다. */
    data object WaitlistFull : JoinAction

    data object AlreadyWaiting : JoinAction

    data class Blocked(
        val reason: JoinBlockReason?,
    ) : JoinAction
}

/** 대기열로도 풀리지 않는 사유인가 — 줄을 서서 해결되는 건 정원뿐이다. */
private val JoinBlockReason.survivesWaitlist: Boolean
    get() = this != JoinBlockReason.FULL

/**
 * [waitlist] 는 정원이 찬 방에서만 필요하다 — 모르면 잠긴 것으로 본다.
 * 못 들어갈 방을 열려 있는 것처럼 보이게 하면 안 된다.
 */
fun ChallengeDetail.joinAction(waitlist: ChallengeWaitlist?): JoinAction =
    when {
        joinable -> JoinAction.Join
        myRole.isMember -> JoinAction.Blocked(JoinBlockReason.ALREADY_JOINED)
        !gate.eligible -> JoinAction.Blocked(JoinBlockReason.TIER_GATE)
        joinBlockReason?.survivesWaitlist == true -> JoinAction.Blocked(joinBlockReason)
        waitlist == null -> JoinAction.Blocked(JoinBlockReason.FULL)
        waitlist.isWaiting -> JoinAction.AlreadyWaiting
        waitlist.isFull -> JoinAction.WaitlistFull
        else -> JoinAction.EnterWaitlist
    }

/** 내 대기가 끝난 이유. 화면 문구와 환불 안내를 가른다. */
enum class WaitlistExitReason(
    val value: String,
) {
    // 자리가 나 선착순으로 자동 참여됨
    PROMOTED("PROMOTED"),

    // 방 시작 — 대기열이 통째로 사라진다
    CHALLENGE_STARTED("CHALLENGE_STARTED"),

    // 본인 취소
    CANCELED("CANCELED"),
    ;

    companion object {
        fun fromValue(value: String?): WaitlistExitReason? = entries.find { it.value == value }
    }
}

/** 대기 종료 통지 (명세 없음 — 푸시 payload 로 가정한다. 서버 계약이 나오면 필드가 바뀐다). */
data class WaitlistExit(
    val challengeId: String,
    val reason: WaitlistExitReason,
    // 모르면 false — 없던 환불을 안내하면 문의가 된다. 금액·시점은 계약에 없어 다루지 않는다.
    val entryFeeRefunded: Boolean = false,
)

/** 대기열이 상한까지 찼다 (서버 에러 코드 미확정). */
class WaitlistFullException : Exception("대기열이 가득 찼어요.")

/** 대기 중 방이 시작돼 대기열이 사라졌다 (서버 에러 코드 미확정). 환불은 서버가 함께 처리한다. */
class WaitlistClosedException : Exception("방이 시작돼 대기가 종료됐어요.")
