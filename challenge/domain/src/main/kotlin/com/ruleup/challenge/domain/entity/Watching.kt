package com.ruleup.challenge.domain.entity

/**
 * 내가 감시자로 등록된 관계 하나 (명세: GET /users/me/watching).
 *
 * 감시자에게는 **실패자 닉네임·챌린지명·루틴명 셋만** 보인다 — 방 상세·랭킹·멤버 목록으로 가는
 * 진입점은 없다(감시자 테크 스펙 6). 그래서 여기에도 challengeId 가 없다.
 *
 * [pushEnabled] 를 꺼도 **알림함 적재는 유지된다** — 푸시만 멈추는 것이고 관계는 그대로다.
 */
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
    /** 아직 통지를 받는 관계인가 — 수신거부한 관계는 토글을 열어 둘 이유가 없다. */
    val isRevoked: Boolean
        get() = status == WatcherStatus.REVOKED
}

/**
 * 수신 설정 결과 (명세: PATCH /users/me/watching/{watcherId}).
 *
 * [reblockUntil] 은 완전 수신거부에서만 온다 — 같은 생성자가 30일간 다시 초대하지 못한다.
 */
data class WatchingUpdate(
    val watcherId: String,
    val status: WatcherStatus?,
    val pushEnabled: Boolean,
    // pushEnabled=false 면 true 고정 — 푸시만 멈추고 알림함은 남는다
    val inboxKept: Boolean,
    val reblockUntil: String?,
)

/** 이미 수신거부한 관계에 다시 거부를 보냈다(서버 409 `ALREADY_REVOKED`). */
class AlreadyRevokedException : Exception("이미 수신을 거부한 항목이에요.")
