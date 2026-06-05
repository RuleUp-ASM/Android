package com.ruleup.data.dto

import com.ruleup.entity.user.CategoryCatalog
import com.ruleup.entity.user.NicknameCheck
import com.ruleup.entity.user.NicknameCheckReason
import com.ruleup.entity.user.Profile
import com.ruleup.entity.user.toInterestCategories
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------- 4.6 닉네임 검사 ----------
@Serializable
data class NicknameCheckResponse(
    @SerialName("valid")
    val valid: Boolean? = null,
    @SerialName("available")
    val available: Boolean? = null,
    @SerialName("reason")
    val reason: String? = null,
)

internal fun NicknameCheckResponse.toDomain(): NicknameCheck =
    NicknameCheck(
        valid = valid ?: false,
        available = available ?: false,
        reason = NicknameCheckReason.fromValue(reason),
    )

// ---------- 4.7 관심 카테고리 마스터 ----------
@Serializable
data class CategoriesResponse(
    @SerialName("maxSelectable")
    val maxSelectable: Int? = null,
    @SerialName("categories")
    val categories: List<CategoryDto>? = null,
)

@Serializable
data class CategoryDto(
    @SerialName("code")
    val code: String? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("emoji")
    val emoji: String? = null,
)

internal fun CategoriesResponse.toDomain(): CategoryCatalog =
    CategoryCatalog(
        maxSelectable = maxSelectable ?: 6,
        categories = categories?.mapNotNull { it.code }.toInterestCategories(),
    )

// ---------- 4.8 / 4.9 프로필 ----------
@Serializable
data class ProfileResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("nicknameChangedAt")
    val nicknameChangedAt: String? = null,
    @SerialName("nicknameChangeableAfter")
    val nicknameChangeableAfter: String? = null,
    @SerialName("mannerTemperature")
    val mannerTemperature: Double? = null,
    @SerialName("interestCategories")
    val interestCategories: List<String>? = null,
    @SerialName("createdAt")
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
        createdAt = createdAt.requireField("createdAt"),
    )

// ---------- 4.10 프로필 이미지 업로드 ----------
@Serializable
data class ProfileImageResponse(
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
)
