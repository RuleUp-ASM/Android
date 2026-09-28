package com.ruleup.profile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// 4.6 닉네임 형식/중복 검사
@Serializable
data class NicknameCheckRequest(
    @SerialName("nickname")
    val nickname: String? = null,
)

// 명세 PATCH /users/me/profile
@Serializable
data class UpdateProfileRequest(
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("interestCategories")
    val interestCategories: List<String>? = null,
    @SerialName("removeProfileImage")
    val removeProfileImage: Boolean? = null,
)
