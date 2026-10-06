package com.ruleup.challenge.domain.entity

/** 챌린지당 · 참여자 기준 무료 감시자 수(초과는 구독 필요). */
const val WATCHER_FREE_LIMIT = 3

/** 감시자 상태 머신. */
enum class WatcherStatus(
    val value: String,
) {
    // 초대 생성(토큰 7일)
    INVITED("INVITED"),

    // 수락+동의 완료(유저 인앱 / 비유저 웹+본인확인)
    CONSENTED("CONSENTED"),

    // 챌린지 진행 중
    ACTIVE("ACTIVE"),

    // 수신거부 / 생성자 해제
    REVOKED("REVOKED"),

    // 7일 미수락 만료
    EXPIRED("EXPIRED"),
    ;

    /** 아직 살아 있는 감시자인가 */
    val isActive: Boolean
        get() = this != REVOKED && this != EXPIRED

    companion object {
        fun fromValue(value: String?): WatcherStatus? = entries.find { it.value == value }
    }
}

/** 감시자 유형. */
enum class WatcherType(
    val value: String,
) {
    USER("USER"),
    NON_USER("NON_USER"),
    ;

    companion object {
        fun fromValue(value: String?): WatcherType? = entries.find { it.value == value }
    }
}

/** 통지 채널. */
enum class WatcherChannel(
    val value: String,
) {
    IN_APP("IN_APP"),
    SMS("SMS"),
    ;

    companion object {
        fun fromValue(value: String?): WatcherChannel? = entries.find { it.value == value }
    }
}

/** 감시자 목록 항목. */
data class Watcher(
    /** 관계 식별자. */
    val watcherId: String?,
    val type: WatcherType,
    val channel: WatcherChannel?,
    val status: WatcherStatus,
    // 유저면 닉네임, 비유저면 null
    val displayName: String?,
    // 비유저 마스킹 연락처(예: 010--5678).
    val contactMasked: String?,
    // INVITED 일 때 토큰 만료 시각
    val expiresAt: String?,
    // REVOKED +30일.
    val reinviteAvailableAt: String?,
) {
    /** 목록에 표시할 이름. */
    val shownName: String
        get() = displayName ?: contactMasked ?: "수락 대기 중인 초대"
}

/** 감시자 목록. */
data class ChallengeWatchers(
    // null 이면 무제한(구독)
    val limit: Int?,
    val watchers: List<Watcher>,
) {
    /** 더 초대할 수 있는 수. 해제·만료된 감시자는 한도를 차지하지 않는다. 무제한이면 null. */
    val remaining: Int?
        get() = limit?.let { (it - watchers.count { watcher -> watcher.status.isActive }).coerceAtLeast(0) }
}

/** 카카오톡 공유 카드 페이로드. */
data class WatcherInviteCard(
    val title: String,
    val description: String,
    val buttonLabel: String,
)

/** 감시자 초대. */
data class WatcherInvitation(
    val invitationId: String?,
    val token: String,
    // 카카오톡 카드 버튼에 실을 초대 링크(웹 동의 페이지 겸용, 7일 만료)
    val inviteUrl: String,
    // ISO datetime
    val expiresAt: String?,
    // 카톡 공유 카드 문구(없으면 클라이언트 기본 문구 사용)
    val kakaoShare: WatcherInviteCard?,
)

/** 무료 3명 초과 초대 시도. */
class WatcherLimitExceededException : RuntimeException("감시자는 챌린지당 무료 $WATCHER_FREE_LIMIT 명까지예요")
