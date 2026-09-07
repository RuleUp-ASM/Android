package com.ruleup.notification.domain.entity

/**
 * 푸시 알림 설정 (명세: GET /users/me/notification-settings).
 *
 * **3계층이고 가장 제한적인 것이 이긴다** — 마스터([pushEnabled]) → 그룹([groups]) →
 * 챌린지 음소거([mutedChallengeIds]). 어느 계층으로 막혀도 **알림 센터에는 그대로 쌓인다.**
 *
 * 설정 행이 없으면 서버가 전부 `true` · 빈 배열로 응답한다 — 가입 시 백필이 없다.
 *
 * OS 푸시 권한은 여기 없다. 서버가 모르는 값이고, 권한을 거부해도 **이 값들은 바뀌지 않는다** —
 * 화면이 권한 상태를 따로 확인해 배너만 얹는다.
 */
data class NotificationSettings(
    // 마스터. 끄면 루틴 리마인더까지 전 푸시가 막힌다
    val pushEnabled: Boolean,
    val groups: NotificationGroupSettings,
    val mutedChallengeIds: List<String>,
) {
    /**
     * 이 그룹의 푸시가 실제로 나가는가. **마스터가 꺼져 있으면 그룹이 켜져 있어도 안 나간다** —
     * 화면이 그룹 토글을 켠 채로 그리면 사용자는 오는 줄 안다.
     */
    fun effectivelyOn(group: NotificationGroup): Boolean {
        if (!pushEnabled) return false
        return groups.of(group)
    }

    fun isMuted(challengeId: String): Boolean = challengeId in mutedChallengeIds
}

/**
 * 그룹 토글 3종. 루틴 리마인더는 여기 없다 — 상시라 그룹 토글 자체가 없다.
 *
 * [marketing] 은 광고성 정보 **수신 동의와 양방향 연동**된 값이다. 여기서 끄면 서버가 같은
 * 트랜잭션에서 동의를 철회한다 — 그래서 화면은 "알림만 끄는 것"처럼 말하면 안 된다.
 */
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
            // 리마인더는 그룹 토글이 없다 — 마스터와 챌린지 음소거만 영향을 준다.
            NotificationGroup.REMINDER -> true
        }
}

/**
 * 설정 변경 요청 (명세: PATCH). **보낸 필드만** 바뀐다.
 *
 * 챌린지 음소거는 여기 없다 — 별도 API(`PUT/DELETE .../mutes/{challengeId}`)다.
 * 허용되지 않은 키를 보내면 서버가 400 으로 막으므로 필드를 늘릴 때 명세를 먼저 본다.
 */
data class NotificationSettingsUpdate(
    val pushEnabled: Boolean? = null,
    val account: Boolean? = null,
    val challenge: Boolean? = null,
    val marketing: Boolean? = null,
)

/**
 * 설정 변경 결과.
 *
 * [marketingConsentSyncedAt] 은 [NotificationSettingsUpdate.marketing] 을 바꿨을 때만 온다 —
 * 광고성 수신 동의를 언제 처리했는지가 법적 기록이라 화면이 그 시각을 안내한다.
 */
data class NotificationSettingsResult(
    val settings: NotificationSettings,
    val marketingConsentSyncedAt: String?,
)

/** 참여하지 않은 챌린지를 음소거하려 했다(서버 400 `CHALLENGE_NOT_JOINED`). */
class ChallengeNotJoinedException : Exception("참여 중인 챌린지만 음소거할 수 있어요.")
