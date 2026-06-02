package com.ruleup.data.profile.dto.response

import com.ruleup.data.common.dto.toInterestCategories
import com.ruleup.domain.profile.model.Profile
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * GET /profile/me 와 PATCH /profile/me 공통 응답.
 * 백엔드가 null 을 넘길 수 있어 모든 필드를 nullable 로 두고 mapper 에서 변환한다.
 */
@Serializable
data class ProfileResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
    @SerialName("nickname_changed_at")
    val nicknameChangedAt: String? = null,
    @SerialName("nickname_changeable_after")
    val nicknameChangeableAfter: String? = null,
    @SerialName("manner_temperature")
    val mannerTemperature: Double? = null,
    @SerialName("interest_categories")
    val interestCategories: List<String>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
)

internal fun ProfileResponse.toDomain(): Profile =
    Profile(
        id = id.requireField("id"),
        nickname = nickname.requireField("nickname"),
        email = email,
        profileImageUrl = profileImageUrl,
        nicknameChangedAt = nicknameChangedAt,
        nicknameChangeableAfter = nicknameChangeableAfter,
        mannerTemperature = mannerTemperature ?: 36.5,
        interestCategories = interestCategories.toInterestCategories(),
        createdAt = createdAt,
    )
