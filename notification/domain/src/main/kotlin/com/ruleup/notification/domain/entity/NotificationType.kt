package com.ruleup.notification.domain.entity

/** 푸시 설정의 그룹. */
enum class NotificationGroup {
    ACCOUNT,
    CHALLENGE,
    MARKETING,
    REMINDER,
}

/** 알림 타입. */
enum class NotificationType(
    val value: String,
    val group: NotificationGroup,
) {
    // 계정
    CHALLENGE_KICKED("CHALLENGE_KICKED", NotificationGroup.ACCOUNT),
    ACCOUNT_SANCTION("ACCOUNT_SANCTION", NotificationGroup.ACCOUNT),
    DORMANCY_NOTICE("DORMANCY_NOTICE", NotificationGroup.ACCOUNT),
    INACTIVE_WITHDRAWAL_NOTICE("INACTIVE_WITHDRAWAL_NOTICE", NotificationGroup.ACCOUNT),

    /** 닉네임·프로필·챌린지 심사 거부가 한 타입으로 합쳐져 있다. */
    MODERATION_REJECTED("MODERATION_REJECTED", NotificationGroup.ACCOUNT),
    CHALLENGE_IMAGE_REMOVED("CHALLENGE_IMAGE_REMOVED", NotificationGroup.ACCOUNT),
    PERMISSION_REGRANT_REQUIRED("PERMISSION_REGRANT_REQUIRED", NotificationGroup.ACCOUNT),
    CHEAT_DETECTED("CHEAT_DETECTED", NotificationGroup.ACCOUNT),
    APPEAL_RESULT("APPEAL_RESULT", NotificationGroup.ACCOUNT),
    TERMS_UPDATED("TERMS_UPDATED", NotificationGroup.ACCOUNT),
    DEVICE_LOGGED_OUT("DEVICE_LOGGED_OUT", NotificationGroup.ACCOUNT),
    CS_ANSWERED("CS_ANSWERED", NotificationGroup.ACCOUNT),

    // 챌린지
    VERIFICATION_RESULT("VERIFICATION_RESULT", NotificationGroup.CHALLENGE),
    CONSECUTIVE_FAILURE_WARNING("CONSECUTIVE_FAILURE_WARNING", NotificationGroup.CHALLENGE),

    /** 시작·종료가 한 타입이다 */
    CHALLENGE_LIFECYCLE("CHALLENGE_LIFECYCLE", NotificationGroup.CHALLENGE),
    WATCHER_INVITATION_EXPIRED("WATCHER_INVITATION_EXPIRED", NotificationGroup.CHALLENGE),
    TIER_CHANGED("TIER_CHANGED", NotificationGroup.CHALLENGE),
    TIER_BOUNDARY_NEAR("TIER_BOUNDARY_NEAR", NotificationGroup.CHALLENGE),
    PENALTY_FAILURE_SHARED("PENALTY_FAILURE_SHARED", NotificationGroup.CHALLENGE),
    WATCHER_REACTION("WATCHER_REACTION", NotificationGroup.CHALLENGE),

    // 상시
    ROUTINE_REMINDER("ROUTINE_REMINDER", NotificationGroup.REMINDER),

    // 마케팅
    MARKETING("MARKETING", NotificationGroup.MARKETING),
    ;

    companion object {
        /** 미지 타입은 null */
        fun fromValue(value: String?): NotificationType? = entries.find { it.value == value }
    }
}
