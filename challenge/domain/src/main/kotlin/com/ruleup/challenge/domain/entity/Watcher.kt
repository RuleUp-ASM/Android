package com.ruleup.challenge.domain.entity

/** 챌린지당 · 참여자 기준 무료 감시자 수(초과는 구독 필요). */
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

/** 감시자 유형(명세: watchers[].type). */
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

/** 통지 채널(명세: watchers[].channel). 룰업 유저 = 인앱 푸시, 비유저 = SMS. */
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

/**
 * 감시자 목록 항목(명세: GET /challenges/{id}/watchers watchers[]).
 * 감시자는 챌린지 × 참여자 단위. 비유저 감시자의 연락처는 초대자에게 원본을 노출하지 않으므로 마스킹 값만 온다.
 */
data class Watcher(
    val watcherId: String,
    val type: WatcherType,
    val channel: WatcherChannel?,
    val status: WatcherStatus,
    // 유저면 닉네임, 비유저면 null
    val displayName: String?,
    // 비유저 마스킹 연락처(예: 010-****-5678). 동의 전(INVITED)엔 null
    val contactMasked: String?,
    // INVITED 일 때 토큰 만료 시각
    val expiresAt: String?,
) {
    /** 목록에 표시할 이름. 동의 전 비유저는 아직 아무 정보도 없다. */
    val shownName: String
        get() = displayName ?: contactMasked ?: "수락 대기 중인 초대"
}

/** 감시자 목록(명세 response). [limit] 은 무료 3, 구독이면 null(무제한). */
data class ChallengeWatchers(
    val limit: Int?,
    val watchers: List<Watcher>,
)

/** 카카오톡 공유 카드 페이로드(명세: kakaoShare). 서버가 문구를 내려준다. */
data class WatcherInviteCard(
    val title: String,
    val description: String,
    val buttonLabel: String,
)

/**
 * 감시자 초대(명세: POST /challenges/{id}/watchers/invitations).
 * [inviteUrl] 을 생성자 본인이 카카오톡 공유로 전달한다(룰업이 직접 발송하지 않음).
 */
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

/**
 * 초대 링크 진입 결과의 수락 가능 상태. 서버 응답의 status(INVITED/CONSENTED/EXPIRED/REVOKED)와
 * blocked(생성자–감시자 30일 차단)를 화면 분기용 단일 상태로 환원한 값.
 */
enum class WatcherInvitationState {
    // 수락 가능(status=INVITED, 차단 아님)
    PENDING,

    // 이미 수락됨(status=CONSENTED)
    ALREADY_ACCEPTED,

    // 7일 경과 만료 — 재초대 필요
    EXPIRED,

    // 해제/수신거부된 초대
    REVOKED,

    // 수신거부 후 동일 생성자 30일 재초대 차단 기간
    BLOCKED,
}

/** 초대 정보(수락 화면 렌더용, 명세: GET /watchers/invitations/{token}). */
data class WatcherInvitationInfo(
    val challengeTitle: String,
    val inviterNickname: String,
    val state: WatcherInvitationState,
    // BLOCKED 일 때 차단 해제 시각(ISO datetime)
    val blockedUntil: String?,
)

/** 무료 3명 초과 초대 시도(명세: 409 WATCHER_LIMIT_EXCEEDED). 프레젠테이션이 구독 안내로 분기한다. */
class WatcherLimitExceededException : RuntimeException("감시자는 챌린지당 무료 $WATCHER_FREE_LIMIT 명까지예요")
