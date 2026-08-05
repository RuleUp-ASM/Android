package com.ruleup.onboarding.data.auth.dto

import com.ruleup.domain.entity.category.toCategories
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.entity.user.User
import com.ruleup.network.dto.requireField
import com.ruleup.onboarding.domain.entity.AuthSession
import com.ruleup.onboarding.domain.entity.OAuthProfile
import com.ruleup.onboarding.domain.entity.OAuthResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginAuthResponse(
    @SerialName("isNewUser")
    val isNewUser: Boolean? = null,
    // ---- 기존 회원 (isNewUser = false) ----
    @SerialName("restored")
    val restored: Boolean? = null,
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("tokenType")
    val tokenType: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Int? = null,
    @SerialName("user")
    val user: UserResponse? = null,
    // ---- 신규 회원 (isNewUser = true) ----
    @SerialName("signupToken")
    val signupToken: String? = null,
    @SerialName("signupTokenExpiresIn")
    val signupTokenExpiresIn: Int? = null,
    @SerialName("oauthProfile")
    val oauthProfile: OAuthProfileResponse? = null,
)

@Serializable
data class UserResponse(
    @SerialName("id")
    val id: String? = null,
    @SerialName("nickname")
    val nickname: String? = null,
    @SerialName("nicknameStatus")
    val nicknameStatus: String? = null,
    @SerialName("profileImageUrl")
    val profileImageUrl: String? = null,
    @SerialName("tier")
    val tier: String? = null,
    @SerialName("score")
    val score: Int? = null,
    @SerialName("displayTier")
    val displayTier: String? = null,
    @SerialName("interestCategories")
    val interestCategories: List<String>? = null,
    @SerialName("onboardingCompleted")
    val onboardingCompleted: Boolean? = null,
    @SerialName("accountStatus")
    val accountStatus: String? = null,
    @SerialName("lockInfo")
    val lockInfo: LockInfoResponse? = null,
)

@Serializable
data class LockInfoResponse(
    @SerialName("reason")
    val reason: String? = null,
    @SerialName("unlockAt")
    val unlockAt: String? = null,
)

@Serializable
data class OAuthProfileResponse(
    @SerialName("email")
    val email: String? = null,
    @SerialName("nicknameHint")
    val nicknameHint: String? = null,
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
    val user: UserResponse? = null,
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
            expiresInSeconds = signupTokenExpiresIn.requireField("signupTokenExpiresIn"),
            profile = oauthProfile.toDomain(),
        )
    } else {
        OAuthResult.ExistingUser(
            session =
                AuthSession(
                    token = token(),
                    user = user.requireField("user").toDomain(),
                ),
            restored = restored ?: false,
        )
    }
}

private fun SocialLoginAuthResponse.token(): Token =
    Token(
        accessToken = accessToken.requireField("accessToken"),
        refreshToken = refreshToken.requireField("refreshToken"),
        tokenType = tokenType ?: DEFAULT_TOKEN_TYPE,
        expiresInSeconds = expiresIn.requireField("expiresIn"),
    )

internal fun OAuthProfileResponse?.toDomain(): OAuthProfile =
    OAuthProfile(
        email = this?.email,
        nicknameHint = this?.nicknameHint,
        profileImageUrlHint = this?.profileImageUrlHint,
    )

/**
 * 필수인 건 `id` 와 `nickname` 뿐이다. 나머지는 미지 값·누락을 안전한 기본으로 떨어뜨린다 —
 * 티어나 계정 상태가 하나 빠졌다고 로그인을 실패시키면, 서버가 enum 을 확장할 때 구버전 앱이
 * 통째로 막힌다.
 */
internal fun UserResponse.toDomain(): User =
    User(
        id = id.requireField("user.id"),
        nickname = nickname.requireField("user.nickname"),
        nicknameStatus = NicknameStatus.fromValue(nicknameStatus),
        profileImageUrl = profileImageUrl,
        tier = Tier.fromValue(tier),
        score = score ?: 0,
        // 표시 티어가 없으면 실제 티어로 떨어뜨린다. 방 입장 판정에 쓰이므로 부풀리면 안 된다.
        displayTier = displayTier?.let(Tier::fromValue) ?: Tier.fromValue(tier),
        interestCategories = interestCategories.toCategories(),
        onboardingCompleted = onboardingCompleted ?: true,
        accountStatus = AccountStatus.fromValue(accountStatus),
        lockInfo = lockInfo?.toDomain(),
    )

internal fun LockInfoResponse.toDomain(): LockInfo? =
    // 사유·해제 시각이 없는 lockInfo 는 표시할 게 없어 없는 것과 같다.
    if (reason == null || unlockAt == null) null else LockInfo(reason = reason, unlockAt = unlockAt)

internal fun SignUpResponse.toAuthSession(): AuthSession =
    AuthSession(
        token =
            Token(
                accessToken = accessToken.requireField("accessToken"),
                refreshToken = refreshToken.requireField("refreshToken"),
                tokenType = tokenType ?: DEFAULT_TOKEN_TYPE,
                expiresInSeconds = expiresIn.requireField("expiresIn"),
            ),
        user = user.requireField("user").toDomain(),
    )

internal fun TokenRefreshResponse.toToken(): Token =
    Token(
        accessToken = accessToken.requireField("accessToken"),
        refreshToken = refreshToken.requireField("refreshToken"),
        tokenType = tokenType ?: DEFAULT_TOKEN_TYPE,
        expiresInSeconds = expiresIn.requireField("expiresIn"),
    )

private const val DEFAULT_TOKEN_TYPE = "Bearer"
