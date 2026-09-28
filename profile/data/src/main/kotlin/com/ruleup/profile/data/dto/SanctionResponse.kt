package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.profile.domain.entity.AdminSanction
import com.ruleup.profile.domain.entity.AutoSanction
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.entity.SanctionTrack
import com.ruleup.profile.domain.entity.SanctionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 제재 통지·이력 (GET /users/me/sanctions)
@Serializable
data class ActiveSanctionResponse(
    @SerialName("sanctionId")
    val sanctionId: String? = null,
    @SerialName("track")
    val track: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("featureCode")
    val featureCode: String? = null,
    @SerialName("reasonCode")
    val reasonCode: String? = null,
    @SerialName("reasonText")
    val reasonText: String? = null,
    @SerialName("startsAt")
    val startsAt: String? = null,
    // BAN 이면 null
    @SerialName("endsAt")
    val endsAt: String? = null,
    @SerialName("reviewRequestable")
    val reviewRequestable: Boolean? = null,
)

@Serializable
data class AdminSanctionResponse(
    @SerialName("sanctionId")
    val sanctionId: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("featureCode")
    val featureCode: String? = null,
    @SerialName("reasonCode")
    val reasonCode: String? = null,
    @SerialName("startsAt")
    val startsAt: String? = null,
    @SerialName("endsAt")
    val endsAt: String? = null,
    @SerialName("reviewStatus")
    val reviewStatus: String? = null,
)

@Serializable
data class AutoSanctionResponse(
    @SerialName("sanctionId")
    val sanctionId: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("challengeId")
    val challengeId: String? = null,
    @SerialName("challengeTitle")
    val challengeTitle: String? = null,
    @SerialName("reasonCode")
    val reasonCode: String? = null,
    @SerialName("permanent")
    val permanent: Boolean? = null,
    @SerialName("rejoinAvailableAt")
    val rejoinAvailableAt: String? = null,
    @SerialName("occurredAt")
    val occurredAt: String? = null,
)

@Serializable
data class SanctionHistoryResponse(
    @SerialName("accountStatus")
    val accountStatus: String? = null,
    @SerialName("activeSanction")
    val activeSanction: ActiveSanctionResponse? = null,
    @SerialName("admin")
    val admin: List<AdminSanctionResponse>? = null,
    @SerialName("auto")
    val auto: List<AutoSanctionResponse>? = null,
)

internal fun SanctionHistoryResponse.toDomain(): SanctionHistory =
    SanctionHistory(
        accountStatus = AccountStatus.fromValue(accountStatus),
        activeSanction = activeSanction?.toDomain(),
        // 식별자가 없어도 제재 사유는 표시한다
        admin = admin.orEmpty().mapNotNull { it.toDomain() },
        auto = auto.orEmpty().mapNotNull { it.toDomain() },
    )

internal fun ActiveSanctionResponse.toDomain(): ActiveSanction? {
    val id = sanctionId?.takeIf { it.isNotBlank() }
    return ActiveSanction(
        sanctionId = id,
        track = SanctionTrack.fromValue(track),
        type = SanctionType.fromValue(type),
        featureCode = featureCode,
        reasonCode = reasonCode,
        reasonText = reasonText,
        startsAt = startsAt,
        endsAt = endsAt,
        // 모르면 재검토 버튼을 열지 않는다
        reviewRequestable = id != null && reviewRequestable == true,
    )
}

internal fun AdminSanctionResponse.toDomain(): AdminSanction? {
    val id = sanctionId?.takeIf { it.isNotBlank() }
    return AdminSanction(
        sanctionId = id,
        type = SanctionType.fromValue(type),
        featureCode = featureCode,
        reasonCode = reasonCode,
        startsAt = startsAt,
        endsAt = endsAt,
        reviewStatus = reviewStatus,
    )
}

internal fun AutoSanctionResponse.toDomain(): AutoSanction? {
    val id = sanctionId?.takeIf { it.isNotBlank() }
    return AutoSanction(
        sanctionId = id,
        type = SanctionType.fromValue(type),
        challengeId = challengeId,
        challengeTitle = challengeTitle,
        reasonCode = reasonCode,
        // 모르면 영구가 아니라고 본다
        permanent = permanent ?: false,
        rejoinAvailableAt = rejoinAvailableAt,
        occurredAt = occurredAt,
    )
}
