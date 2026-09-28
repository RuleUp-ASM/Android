package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.MemberProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 타인 프로필 응답. */
@Serializable
data class MemberProfileResponse(
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("displayTier")
    val displayTier: String? = null,
    @SerialName("completedChallengeCount")
    val completedChallengeCount: Int? = null,
    @SerialName("withdrawn")
    val withdrawn: Boolean? = null,
    @SerialName("blocked")
    val blocked: Boolean? = null,
)

internal fun MemberProfileResponse.toDomain(): MemberProfile =
    MemberProfile(
        user =
            com.ruleup.domain.entity.user
                .User(userId.requireField("userId"), nickname.requireField("nickname"), profileImageUrl),
        tier = Tier.fromValue(displayTier),
        // 완주 개수가 비면 0 이다
        completedChallengeCount = completedChallengeCount ?: 0,
        withdrawn = withdrawn ?: false,
        blocked = blocked ?: false,
    )
