package com.ruleup.data.dto.response

import com.ruleup.core.model.InterestCategory
import com.ruleup.core.model.Tokens
import com.ruleup.core.model.User
import com.ruleup.domain.model.AuthSession
import com.ruleup.domain.model.OAuthProfile
import com.ruleup.domain.model.OAuthResult
import com.ruleup.network.dto.requireField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val DEFAULT_TOKEN_TYPE = "Bearer"
private const val DEFAULT_MANNER_TEMPERATURE = 36.5

@Serializable
data class AuthResponse(
    @SerialName("is_new_user")
    val isNewUser: Boolean? = null,
    // 기존 사용자 (is_new_user = false)
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
    @SerialName("expires_in")
    val expiresIn: Int? = null,
    @SerialName("user")
    val user: UserDto? = null,
    // 신규 사용자 (is_new_user = true)
    @SerialName("signup_token")
    val signupToken: String? = null,
    @SerialName("signup_token_expires_in")
    val signupTokenExpiresIn: Int? = null,
    @SerialName("oauth_profile")
    val oauthProfile: OAuthProfileDto? = null,
)


@Serializable
data class UserDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
    @SerialName("manner_temperature")
    val mannerTemperature: Double? = null,
    @SerialName("interest_categories")
    val interestCategories: List<String>? = null,
)

@Serializable
data class OAuthProfileDto(
    @SerialName("email")
    val email: String? = null,
    @SerialName("profile_image_url_hint")
    val profileImageUrlHint: String? = null,
)

/** 알 수 없는 카테고리 코드는 조용히 버린다. */
internal fun List<String>?.toInterestCategories(): List<InterestCategory> =
    this.orEmpty().mapNotNull(InterestCategory::fromValue)

internal fun UserDto.toDomain(): User =
    User(
        id = id.requireField("user.id"),
        nickname = nickname.requireField("user.nickname"),
        email = email,
        profileImageUrl = profileImageUrl,
        mannerTemperature = mannerTemperature ?: DEFAULT_MANNER_TEMPERATURE,
        interestCategories = interestCategories.toInterestCategories(),
    )

private fun OAuthProfileDto?.toDomain(): OAuthProfile =
    OAuthProfile(
        email = this?.email,
        profileImageUrlHint = this?.profileImageUrlHint,
    )

internal fun AuthResponse.toOAuthResult(): OAuthResult {
    // is_new_user 가 비어 오면 signup_token 유무로 판별한다.
    val isNew = isNewUser ?: (signupToken != null)
    return if (isNew) {
        OAuthResult.NewUser(
            signupToken = signupToken.requireField("signup_token"),
            signupTokenExpiresInSeconds = signupTokenExpiresIn.requireField("signup_token_expires_in"),
            oauthProfile = oauthProfile.toDomain(),
        )
    } else {
        OAuthResult.ExistingUser(
            session = AuthSession(
                tokens = Tokens(
                    accessToken = accessToken.requireField("access_token"),
                    refreshToken = refreshToken.requireField("refresh_token"),
                    tokenType = tokenType ?: DEFAULT_TOKEN_TYPE,
                    expiresInSeconds = expiresIn.requireField("expires_in"),
                ),
                user = user.requireField("user").toDomain(),
            ),
        )
    }
}
