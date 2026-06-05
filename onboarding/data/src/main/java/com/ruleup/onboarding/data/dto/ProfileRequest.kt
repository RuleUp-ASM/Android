package com.ruleup.onboarding.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 4.6 닉네임 형식/중복 검사
@Serializable
data class NicknameCheckRequest(
    @SerialName("nickname")
    val nickname: String? = null,
)

// 4.9 프로필 수정 (변경할 필드만 전달)
@Serializable
data class UpdateProfileRequest(
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("interestCategories")
    val interestCategories: List<String>? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
)
