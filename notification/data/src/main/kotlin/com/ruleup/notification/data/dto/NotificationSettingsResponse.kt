package com.ruleup.notification.data.dto

import com.ruleup.network.dto.ApiException
import com.ruleup.notification.domain.entity.ChallengeNotJoinedException
import com.ruleup.notification.domain.entity.NotificationGroupSettings
import com.ruleup.notification.domain.entity.NotificationSettings
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.entity.NotificationSettingsUpdate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 알림 설정 (GET · PATCH /users/me/notification-settings)
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
    /** 구 계약의 평평한 마케팅 토글. */
    @SerialName("marketing")
    val legacyMarketing: Boolean? = null,
)

/** 설정 응답 → entity. */
internal fun NotificationSettingsResponse.toDomain(): NotificationSettings =
    NotificationSettings(
        pushEnabled = pushEnabled ?: true,
        groups =
            NotificationGroupSettings(
                account = groups?.account ?: true,
                challenge = groups?.challenge ?: true,
                // groups 가 없으면 구 계약의 평평한 값을 쓴다.
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

/** 바꾸려는 필드만 싣는다. */
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
    // 마케팅을 바꿨을 때만 온다
    @SerialName("marketingConsentSyncedAt")
    val marketingConsentSyncedAt: String? = null,
    // 구 계약은 설정을 봉투 없이 그대로 준다.
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

/** 음소거 실패를 화면이 분기할 수 있는 타입으로 옮긴다 */
internal fun ApiException.toMuteFailure(): Throwable =
    when (code) {
        "CHALLENGE_NOT_JOINED" -> ChallengeNotJoinedException()
        else -> this
    }
