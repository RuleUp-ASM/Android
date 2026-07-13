package com.ruleup.challenge.domain.entity

/** 챌린지당 무료 감시자 수(초과는 구독 필요). */
const val WATCHER_FREE_LIMIT = 3

/**
 * 감시자 상태 머신(감시자 통지 스펙 §4).
 * INVITED → CONSENTED → ACTIVE → REVOKED, INVITED → EXPIRED. 발송은 ACTIVE 에서만.
 */
enum class WatcherStatus(
    val value: String,
) {
    // 초대 생성(토큰 7일)
    INVITED("INVITED"),

    // 수락+동의 완료(유저 인앱 / 비유저 웹+본인확인)
    CONSENTED("CONSENTED"),

    // 챌린지 진행 중 — 실패 시 통지 대상
    ACTIVE("ACTIVE"),

    // 수신거부 / 생성자 해제
    REVOKED("REVOKED"),

    // 7일 미수락 만료
    EXPIRED("EXPIRED"),
    ;

    companion object {
        fun fromValue(value: String?): WatcherStatus? = entries.find { it.value == value }
    }
}

/** 통지 채널. 룰업 유저 = 인앱 푸시, 비유저 = SMS(기본)/이메일. */
enum class WatcherChannel(
    val value: String,
) {
    IN_APP("IN_APP"),
    SMS("SMS"),
    EMAIL("EMAIL"),
    ;

    companion object {
        fun fromValue(value: String?): WatcherChannel? = entries.find { it.value == value }
    }
}

/**
 * 감시자 목록 항목(명세: GET /challenges/{id}/watchers).
 * 비유저 감시자의 연락처는 생성자에게 원본을 노출하지 않으므로 마스킹 값만 온다.
 */
data class Watcher(
    val watcherId: String,
    // 룰업 유저 감시자의 닉네임(비유저는 null)
    val nickname: String?,
    // 비유저 감시자의 마스킹된 연락처(유저는 null)
    val maskedContact: String?,
    val status: WatcherStatus,
    val channel: WatcherChannel?,
) {
    /** 목록에 표시할 이름. */
    val displayName: String
        get() = nickname ?: maskedContact ?: "알 수 없음"
}

/**
 * 감시자 초대(명세: POST /challenges/{id}/watchers/invitations).
 * [inviteUrl] 을 생성자 본인이 카카오톡 공유로 전달한다(룰업이 직접 발송하지 않음).
 */
data class WatcherInvitation(
    val token: String,
    // 카카오톡 카드 버튼에 실을 초대 링크(웹 동의 페이지 겸용, 7일 만료)
    val inviteUrl: String,
    // ISO datetime
    val expiresAt: String?,
)

/** 초대 링크 진입 시 토큰 검증 결과(명세: GET /watchers/invitations/{token}). */
enum class WatcherInvitationState(
    val value: String,
) {
    // 수락 가능
    PENDING("PENDING"),

    // 7일 경과 만료 — 재초대 필요
    EXPIRED("EXPIRED"),

    // 이미 수락됨((챌린지, 계정) 유니크)
    ALREADY_ACCEPTED("ALREADY_ACCEPTED"),

    // 수신거부 후 동일 생성자 30일 재초대 차단 기간
    BLOCKED("BLOCKED"),
    ;

    companion object {
        fun fromValue(value: String?): WatcherInvitationState? = entries.find { it.value == value }
    }
}

/** 초대 정보(수락 화면 렌더용): 누가 어떤 챌린지의 감시자로 초대했는지. */
data class WatcherInvitationInfo(
    val challengeId: String,
    val challengeTitle: String,
    val ownerNickname: String,
    val state: WatcherInvitationState,
)

/** 무료 3명 초과 초대 시도(구독 필요). 프레젠테이션이 구독 안내로 분기한다. */
class WatcherLimitExceededException : RuntimeException("감시자는 챌린지당 무료 $WATCHER_FREE_LIMIT 명까지예요")
