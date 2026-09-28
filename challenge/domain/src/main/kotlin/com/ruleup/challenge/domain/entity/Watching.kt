package com.ruleup.challenge.domain.entity

/** 내가 감시자로 등록된 관계 하나. */
data class Watching(
    val watcherId: String,
    // 심사 대체 규칙이 적용된 제목
    val challengeTitle: String,
    // 감시 대상(초대한 사람) 닉네임
    val ownerNickname: String,
    val status: WatcherStatus?,
    val pushEnabled: Boolean,
    // 동의 시각 ISO-8601
    val consentAt: String?,
) {
    /** 아직 통지를 받는 관계인가 */
    val isRevoked: Boolean
        get() = status == WatcherStatus.REVOKED
}

/** 초대 수락 결과. */
data class WatcherAcceptance(
    val watcherId: String,
    val status: WatcherStatus?,
    val channel: WatcherChannel?,
)

/** 초대 토큰이 7일을 넘겼다(서버 410 `INVITATION_EXPIRED`). */
class InvitationExpiredException : Exception("초대가 만료됐어요. 다시 초대해 달라고 해주세요.")

/** 이미 수락한 초대다(서버 409 `ALREADY_CONSENTED` · `ALREADY_WATCHER`). */
class AlreadyConsentedException : Exception("이미 수락한 초대예요.")

/** 자기 자신을 감시자로 수락할 수 없다(서버 400 `CANNOT_WATCH_SELF`). */
class CannotWatchSelfException : Exception("내가 만든 챌린지의 감시자는 될 수 없어요.")

/** 상대가 수신을 거부해 30일간 다시 지정할 수 없다(서버 409 `WATCHER_BLOCKED`). */
class WatcherBlockedException : Exception("지금은 이 초대를 수락할 수 없어요.")

/** 유효하지 않거나 없는 초대 토큰. */
class InvitationInvalidException : Exception("초대 링크를 확인할 수 없어요.")
