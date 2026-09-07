package com.ruleup.notification.data.dto

import com.ruleup.network.dto.ApiException
import com.ruleup.notification.domain.entity.ChallengeNotJoinedException
import com.ruleup.notification.domain.entity.NotificationGroupSettings
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 알림 설정 (GET · PATCH /users/me/notification-settings) ----------
@Serializable
data class NotificationGroupsResponse(
    @SerialName("account")
    val account: Boolean? = null,
    @SerialName("challenge")
    val challenge: Boolean? = null,
    @SerialName("marketing")
    val marketing: Boolean? = null,
)

@Serializable
data class NotificationSettingsResponse(
    @SerialName("pushEnabled")
    val pushEnabled: Boolean? = null,
    @SerialName("groups")
    val groups: NotificationGroupsResponse? = null,
    @SerialName("mutedChallengeIds")
    val mutedChallengeIds: List<String>? = null,
    /**
     * 구 계약의 평평한 마케팅 토글.
     *
     * 배포된 서버는 아직 `{types, mutedChallengeIds, marketing}` 을 준다(2026-09-07 확인).
     * `groups` 가 없을 때 마케팅만이라도 실제 값으로 그리려고 받아 둔다 — 계정·챌린지는
     * 구 모델에 대응하는 값이 없어 지어내지 않는다.
     */
    @SerialName("marketing")
    val legacyMarketing: Boolean? = null,
)

/**
 * 설정 응답 → entity.
 *
 * **없는 값은 켜짐으로 본다.** 서버가 "설정 행이 없으면 전부 true" 로 응답하는 것과 같은 방향이고,
 * 꺼진 것처럼 그렸다가 알림이 오면 설정 화면이 거짓말한 게 된다.
 */
internal fun NotificationSettingsResponse.toDomain(): NotificationSettings =
    NotificationSettings(
        pushEnabled = pushEnabled ?: true,
        groups =
            NotificationGroupSettings(
                account = groups?.account ?: true,
                challenge = groups?.challenge ?: true,
                // groups 가 없으면 구 계약의 평평한 값을 쓴다. 그것도 없으면 켜짐.
                marketing = groups?.marketing ?: legacyMarketing ?: true,
            ),
        mutedChallengeIds = mutedChallengeIds.orEmpty(),
    )

@Serializable
data class NotificationGroupsRequest(
    @SerialName("account")
    val account: Boolean? = null,
    @SerialName("challenge")
    val challenge: Boolean? = null,
    @SerialName("marketing")
    val marketing: Boolean? = null,
)

@Serializable
data class NotificationSettingsRequest(
    @SerialName("pushEnabled")
    val pushEnabled: Boolean? = null,
    @SerialName("groups")
    val groups: NotificationGroupsRequest? = null,
)

/**
 * 바꾸려는 필드만 싣는다.
 *
 * 서버가 **허용되지 않은 키를 400 으로 막으므로**(무시하지 않는다) 빈 `groups` 객체를 실어 보내지
 * 않는다 — 그룹을 하나도 안 바꾸면 키 자체를 뺀다.
 */
internal fun NotificationSettingsUpdate.toRequest(): NotificationSettingsRequest {
    val groups =
        if (account == null && challenge == null && marketing == null) {
            null
        } else {
            NotificationGroupsRequest(account = account, challenge = challenge, marketing = marketing)
        }
    return NotificationSettingsRequest(pushEnabled = pushEnabled, groups = groups)
}

@Serializable
data class NotificationSettingsUpdateResponse(
    @SerialName("settings")
    val settings: NotificationSettingsResponse? = null,
    // 마케팅을 바꿨을 때만 온다 — 수신 동의 처리 시각이라 법적 기록이다
    @SerialName("marketingConsentSyncedAt")
    val marketingConsentSyncedAt: String? = null,
    // 구 계약은 설정을 봉투 없이 그대로 준다. 그때도 반영값을 읽을 수 있게 둔다
    @SerialName("pushEnabled")
    val flatPushEnabled: Boolean? = null,
    @SerialName("groups")
    val flatGroups: NotificationGroupsResponse? = null,
    @SerialName("mutedChallengeIds")
    val flatMutedChallengeIds: List<String>? = null,
    @SerialName("marketing")
    val flatMarketing: Boolean? = null,
)

internal fun NotificationSettingsUpdateResponse.toDomain(): NotificationSettingsResult =
    NotificationSettingsResult(
        settings =
            (
                settings ?: NotificationSettingsResponse(
                    pushEnabled = flatPushEnabled,
                    groups = flatGroups,
                    mutedChallengeIds = flatMutedChallengeIds,
                    legacyMarketing = flatMarketing,
                )
            ).toDomain(),
        marketingConsentSyncedAt = marketingConsentSyncedAt,
    )

/** 음소거 실패를 화면이 분기할 수 있는 타입으로 옮긴다 — 참여하지 않은 방은 목록을 갱신해야 한다. */
internal fun ApiException.toMuteFailure(): Throwable =
    when (code) {
        "CHALLENGE_NOT_JOINED" -> ChallengeNotJoinedException()
        else -> this
    }
