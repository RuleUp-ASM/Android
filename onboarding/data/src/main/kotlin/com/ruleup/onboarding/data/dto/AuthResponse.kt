package com.ruleup.onboarding.data.dto

import com.ruleup.entity.user.AuthSession
import com.ruleup.entity.user.Token
import com.ruleup.entity.user.User
import com.ruleup.entity.user.toInterestCategories
import com.ruleup.network.dto.requireField
import com.ruleup.onboarding.domain.entity.OAuthProfile
import com.ruleup.onboarding.domain.entity.OAuthResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginAuthResponse(
    @SerialName("isNewUser")
    val isNewUser: Boolean? = null,
    // 기존 사용자 (isNewUser = false)
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("tokenType")
    val tokenType: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Int? = null,
    @SerialName("user")
    val user: UserDto? = null,
    // 신규 사용자 (isNewUser = true)
    @SerialName("signupToken")
    val signupToken: String? = null,
    @SerialName("signupTokenExpiresIn")
    val signupTokenExpiresIn: Int? = null,
    @SerialName("oauthProfile")
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
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("mannerTemperature")
    val mannerTemperature: Double? = null,
    @SerialName("interestCategories")
    val interestCategories: List<String>? = null,
)

@Serializable
data class OAuthProfileDto(
    @SerialName("email")
    val email: String? = null,
    @SerialName("profileImageUrlHint")
    val profileImageUrlHint: String? = null,
)

@Serializable
data class SignUpResponse(
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("tokenType")
    val tokenType: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Int? = null,
    @SerialName("user")
    val user: UserDto? = null,
)

@Serializable
data class TokenRefreshResponse(
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("tokenType")
    val tokenType: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Int? = null,
)

internal fun SocialLoginAuthResponse.toOAuthResult(): OAuthResult {
    // isNewUser 가 비어 오면 signupToken 유무로 판별한다.
    val isNew = isNewUser ?: (signupToken != null)
    return if (isNew) {
        OAuthResult.NewUser(
            signupToken = signupToken.requireField("signupToken"),
            oauthProfile = oauthProfile.toDomain(),
            signupTokenExpireInSeconds = signupTokenExpiresIn.requireField("signupTokenExpiresIn"),
        )
    } else {
        OAuthResult.ExistingUser(
            session =
                AuthSession(
                    token =
                        Token(
                            accessToken = accessToken.requireField("accessToken"),
                            refreshToken = refreshToken.requireField("refreshToken"),
                            tokenType = tokenType ?: "Bearer",
                            expiresInSeconds = expiresIn.requireField("expiresIn"),
                        ),
                    user = user.requireField("user").toDomain(),
                ),
        )
    }
}

internal fun OAuthProfileDto?.toDomain(): OAuthProfile =
    OAuthProfile(
        email = this?.email,
        profileImageUrlHint = this?.profileImageUrlHint,
    )

internal fun UserDto.toDomain(): User =
    User(
        id = id.requireField("user.id"),
        nickname = nickname.requireField("nickname"),
        email = email,
        profileImageUrl = profileImageUrl,
        mannerTemperature = mannerTemperature ?: 36.5,
        interestCategories = interestCategories.toInterestCategories(),
    )

internal fun SignUpResponse.toAuthSession(): AuthSession =
    AuthSession(
        token =
            Token(
                accessToken = accessToken.requireField("accessToken"),
                refreshToken = refreshToken.requireField("refreshToken"),
                tokenType = tokenType ?: "Bearer",
                expiresInSeconds = expiresIn.requireField("expiresIn"),
            ),
        user = user.requireField("user").toDomain(),
    )

internal fun TokenRefreshResponse.toToken(): Token =
    Token(
        accessToken = accessToken.requireField("accessToken"),
        refreshToken = refreshToken.requireField("refreshToken"),
        tokenType = tokenType ?: "Bearer",
        expiresInSeconds = expiresIn.requireField("expiresIn"),
    )
