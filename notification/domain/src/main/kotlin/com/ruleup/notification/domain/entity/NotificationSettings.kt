package com.ruleup.notification.domain.entity

/** 푸시 알림 설정. */
data class NotificationSettings(
    // 마스터.
    val pushEnabled: Boolean,
    val groups: NotificationGroupSettings,
    val mutedChallengeIds: List<String>,
) {
    /** 이 그룹의 푸시가 실제로 나가는가. */
    fun effectivelyOn(group: NotificationGroup): Boolean {
        if (!pushEnabled) return false
        return groups.of(group)
    }

    fun isMuted(challengeId: String): Boolean = challengeId in mutedChallengeIds
}

/** 그룹 토글 3종. */
data class NotificationGroupSettings(
    val account: Boolean,
    val challenge: Boolean,
    val marketing: Boolean,
) {
    fun of(group: NotificationGroup): Boolean =
        when (group) {
            NotificationGroup.ACCOUNT -> account
            NotificationGroup.CHALLENGE -> challenge
            NotificationGroup.MARKETING -> marketing
            // 리마인더는 그룹 토글이 없다
            NotificationGroup.REMINDER -> true
        }
}

/** 설정 변경 요청. */
data class NotificationSettingsUpdate(
    val pushEnabled: Boolean? = null,
    val account: Boolean? = null,
    val challenge: Boolean? = null,
    val marketing: Boolean? = null,
)

/** 설정 변경 결과. */
data class NotificationSettingsResult(
    val settings: NotificationSettings,
    val marketingConsentSyncedAt: String?,
)

/** 참여하지 않은 챌린지를 음소거하려 했다(서버 400 `CHALLENGE_NOT_JOINED`). */
class ChallengeNotJoinedException : Exception("참여 중인 챌린지만 음소거할 수 있어요.")
