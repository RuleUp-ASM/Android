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
 * 알림 타입 (테크 스펙 8의 레지스트리).
 *
 * 타입마다 **그룹**과 **딥링크**가 정해져 있다. 딥링크는 서버가 응답에 실어 주므로 클라가
 * 조립하지 않는다 — 여기 [group] 만 두는 이유는 목록의 뱃지 문구와 설정 화면의 설명이
 * 그룹 단위로 갈리기 때문이다.
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
    KICK_CONFIRMED("KICK_CONFIRMED", NotificationGroup.ACCOUNT),
    ACCOUNT_LOCK_CHANGED("ACCOUNT_LOCK_CHANGED", NotificationGroup.ACCOUNT),
    DORMANCY_NOTICE("DORMANCY_NOTICE", NotificationGroup.ACCOUNT),
    INACTIVE_WITHDRAWAL_NOTICE("INACTIVE_WITHDRAWAL_NOTICE", NotificationGroup.ACCOUNT),
    MODERATION_REJECTED_NICKNAME("MODERATION_REJECTED_NICKNAME", NotificationGroup.ACCOUNT),
    MODERATION_REJECTED_PROFILE("MODERATION_REJECTED_PROFILE", NotificationGroup.ACCOUNT),
    MODERATION_REJECTED_CHALLENGE("MODERATION_REJECTED_CHALLENGE", NotificationGroup.ACCOUNT),
    PERMISSION_REQUIRED("PERMISSION_REQUIRED", NotificationGroup.ACCOUNT),
    CHEAT_DETECTED("CHEAT_DETECTED", NotificationGroup.ACCOUNT),
    APPEAL_RESULT("APPEAL_RESULT", NotificationGroup.ACCOUNT),
    TERMS_UPDATED("TERMS_UPDATED", NotificationGroup.ACCOUNT),
    DEVICE_LOGGED_OUT("DEVICE_LOGGED_OUT", NotificationGroup.ACCOUNT),

    // ---------- 챌린지 ----------
    VERIFICATION_RESULT("VERIFICATION_RESULT", NotificationGroup.CHALLENGE),
    CONSECUTIVE_FAIL_WARNING("CONSECUTIVE_FAIL_WARNING", NotificationGroup.CHALLENGE),
    CHALLENGE_STARTED("CHALLENGE_STARTED", NotificationGroup.CHALLENGE),
    CHALLENGE_ENDED("CHALLENGE_ENDED", NotificationGroup.CHALLENGE),
    CHALLENGE_IMAGE_DELETED("CHALLENGE_IMAGE_DELETED", NotificationGroup.CHALLENGE),
    WATCHER_INVITE_EXPIRED("WATCHER_INVITE_EXPIRED", NotificationGroup.CHALLENGE),
    TIER_CHANGED("TIER_CHANGED", NotificationGroup.CHALLENGE),
    TIER_BOUNDARY_NEAR("TIER_BOUNDARY_NEAR", NotificationGroup.CHALLENGE),
    PENALTY_FAIL_SHARED("PENALTY_FAIL_SHARED", NotificationGroup.CHALLENGE),
    CHEER_REACTION("CHEER_REACTION", NotificationGroup.CHALLENGE),

    // ---------- 상시 ----------
    ROUTINE_REMINDER("ROUTINE_REMINDER", NotificationGroup.REMINDER),

    // ---------- 마케팅 ----------
    MARKETING_CAMPAIGN("MARKETING_CAMPAIGN", NotificationGroup.MARKETING),
    ;

    companion object {
        /** 미지 타입은 null — 목록에서 빼지 않고 뱃지만 비운다. 적재된 고지를 숨기면 안 된다. */
        fun fromValue(value: String?): NotificationType? = entries.find { it.value == value }
    }
}
