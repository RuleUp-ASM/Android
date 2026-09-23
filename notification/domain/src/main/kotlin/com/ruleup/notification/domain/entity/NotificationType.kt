package com.ruleup.notification.domain.entity

/**
 * 푸시 설정의 그룹 (알림 테크 스펙 4). 설정은 3계층이고 **가장 제한적인 것이 이긴다.**
 *
 * [REMINDER] 는 그룹 토글이 없다 — 루틴 리마인더는 상시라 마스터와 챌린지 음소거만 영향을 준다.
 * 목록에서 이 값을 그룹 토글에 매핑하면 끌 수 없는 토글이 하나 생긴다.
 */
enum class NotificationGroup {
    ACCOUNT,
    CHALLENGE,
    MARKETING,
    REMINDER,
}

/**
 * 알림 타입 (백엔드 테크 스펙 5-1 의 레지스트리 23종).
 *
 * 타입마다 **그룹**과 **딥링크**가 정해져 있다. 딥링크는 서버가 응답에 실어 주므로 클라가
 * 조립하지 않는다 — 여기 [group] 만 두는 이유는 목록의 뱃지 문구와 설정 화면의 설명이
 * 그룹 단위로 갈리기 때문이다.
 *
 * **[value] 는 서버가 적재한 `notifications.type` 그대로여야 한다.** 이름을 짐작해 적으면
 * [fromValue] 가 null 을 돌려주고, 그 타입의 알림만 조용히 뱃지 없이 렌더된다 — 목록에는
 * 남으니 QA 로도 늦게 잡힌다. 실제로 12종이 그렇게 어긋나 있었다(2026-09-22 QA).
 *
 * 운영자 공지(`ANNOUNCEMENT`)는 여기 없다. 별도 탭([NotificationTab.ANNOUNCEMENT])으로 갈리고
 * 그 탭은 전부 공지라 뱃지로 가를 것이 없다.
 *
 * **모르는 타입도 목록에 세운다** — 서버가 타입을 하나 추가했다고 그 알림이 화면에서 사라지면
 * 법적 고지가 성립한 항목을 사용자가 못 보게 된다. 그래서 [fromValue] 는 null 을 돌려주고,
 * 화면은 제목·본문만으로 그린다.
 */
enum class NotificationType(
    val value: String,
    val group: NotificationGroup,
) {
    // ---------- 계정 ----------
    CHALLENGE_KICKED("CHALLENGE_KICKED", NotificationGroup.ACCOUNT),
    ACCOUNT_SANCTION("ACCOUNT_SANCTION", NotificationGroup.ACCOUNT),
    DORMANCY_NOTICE("DORMANCY_NOTICE", NotificationGroup.ACCOUNT),
    INACTIVE_WITHDRAWAL_NOTICE("INACTIVE_WITHDRAWAL_NOTICE", NotificationGroup.ACCOUNT),

    /** 닉네임·프로필·챌린지 심사 거부가 한 타입으로 합쳐져 있다. 대상은 딥링크가 가른다. */
    MODERATION_REJECTED("MODERATION_REJECTED", NotificationGroup.ACCOUNT),
    CHALLENGE_IMAGE_REMOVED("CHALLENGE_IMAGE_REMOVED", NotificationGroup.ACCOUNT),
    PERMISSION_REGRANT_REQUIRED("PERMISSION_REGRANT_REQUIRED", NotificationGroup.ACCOUNT),
    CHEAT_DETECTED("CHEAT_DETECTED", NotificationGroup.ACCOUNT),
    APPEAL_RESULT("APPEAL_RESULT", NotificationGroup.ACCOUNT),
    TERMS_UPDATED("TERMS_UPDATED", NotificationGroup.ACCOUNT),
    DEVICE_LOGGED_OUT("DEVICE_LOGGED_OUT", NotificationGroup.ACCOUNT),
    CS_ANSWERED("CS_ANSWERED", NotificationGroup.ACCOUNT),

    // ---------- 챌린지 ----------
    VERIFICATION_RESULT("VERIFICATION_RESULT", NotificationGroup.CHALLENGE),
    CONSECUTIVE_FAILURE_WARNING("CONSECUTIVE_FAILURE_WARNING", NotificationGroup.CHALLENGE),

    /** 시작·종료가 한 타입이다 — 어느 쪽인지는 본문이 말한다. */
    CHALLENGE_LIFECYCLE("CHALLENGE_LIFECYCLE", NotificationGroup.CHALLENGE),
    WATCHER_INVITATION_EXPIRED("WATCHER_INVITATION_EXPIRED", NotificationGroup.CHALLENGE),
    TIER_CHANGED("TIER_CHANGED", NotificationGroup.CHALLENGE),
    TIER_BOUNDARY_NEAR("TIER_BOUNDARY_NEAR", NotificationGroup.CHALLENGE),
    PENALTY_FAILURE_SHARED("PENALTY_FAILURE_SHARED", NotificationGroup.CHALLENGE),
    WATCHER_REACTION("WATCHER_REACTION", NotificationGroup.CHALLENGE),

    // ---------- 상시 ----------
    ROUTINE_REMINDER("ROUTINE_REMINDER", NotificationGroup.REMINDER),

    // ---------- 마케팅 ----------
    MARKETING("MARKETING", NotificationGroup.MARKETING),
    ;

    companion object {
        /** 미지 타입은 null — 목록에서 빼지 않고 뱃지만 비운다. 적재된 고지를 숨기면 안 된다. */
        fun fromValue(value: String?): NotificationType? = entries.find { it.value == value }
    }
}
