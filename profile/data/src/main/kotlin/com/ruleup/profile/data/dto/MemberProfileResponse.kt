package com.ruleup.profile.data.dto

import com.ruleup.domain.entity.user.Tier
import com.ruleup.network.dto.requireField
import com.ruleup.profile.domain.entity.MemberProfile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 타인 프로필 응답 (명세 GET /users/{userId}/profile).
 *
 * 닉네임·사진은 **서버가 이미 대체 규칙을 적용해** 내려준다(심사 중이면 직전 승인본, 차단했으면
 * 임시 닉네임·기본 이미지). 앱이 다시 가리지 않는다 — 두 번 가리면 서버 규칙이 바뀔 때 어긋난다.
 */
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
        userId = userId.requireField("userId"),
        nickname = nickname.requireField("nickname"),
        profileImageUrl = profileImageUrl,
        tier = Tier.fromValue(displayTier),
        // 완주 개수가 비면 0 이다 — 한 번도 끝내지 않은 사용자와 값이 같고, 그게 사실이다.
        completedChallengeCount = completedChallengeCount ?: 0,
        withdrawn = withdrawn ?: false,
        blocked = blocked ?: false,
    )
