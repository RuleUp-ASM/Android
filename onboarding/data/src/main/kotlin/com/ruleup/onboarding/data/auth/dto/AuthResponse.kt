package com.ruleup.onboarding.data.auth.dto

import com.ruleup.domain.entity.category.toCategories
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.entity.user.LockInfo
import com.ruleup.domain.entity.user.NicknameStatus
import com.ruleup.domain.entity.user.Tier
import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.entity.user.User
import com.ruleup.domain.token.RefreshedSession
import com.ruleup.network.dto.ApiException
import com.ruleup.network.dto.requireField
import com.ruleup.onboarding.domain.entity.AuthFailure
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
    // 갱신 응답도 사용자 식별자를 함께 준다. 없으면 호출부가 기존 값을 유지한다.
    @SerialName("userId")
    val userId: String? = null,
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

internal fun TokenRefreshResponse.toRefreshedSession(): RefreshedSession =
    RefreshedSession(
        token =
            Token(
                accessToken = accessToken.requireField("accessToken"),
                refreshToken = refreshToken.requireField("refreshToken"),
                tokenType = tokenType ?: DEFAULT_TOKEN_TYPE,
                expiresInSeconds = expiresIn.requireField("expiresIn"),
            ),
        userId = userId?.takeIf { it.isNotBlank() },
    )

/**
 * 실패 응답의 `error.code` 를 [AuthFailure] 로 옮긴다.
 *
 * 모르는 코드는 [AuthFailure.UNKNOWN] 이다 — 서버가 코드를 추가해도 앱이 터지지 않고 일반 안내로
 * 떨어진다.
 */
internal fun ApiException.toAuthFailure(): AuthFailure = AUTH_FAILURE_CODES[code] ?: AuthFailure.UNKNOWN

private val AUTH_FAILURE_CODES: Map<String, AuthFailure> =
    mapOf(
        "LOGIN_FAILED" to AuthFailure.LOGIN_FAILED,
        "INVALID_REDIRECT_URI" to AuthFailure.INVALID_REDIRECT_URI,
        "INVALID_DEVICE_INFO" to AuthFailure.INVALID_DEVICE_INFO,
        "LOGIN_PROVIDER_UNAVAILABLE" to AuthFailure.PROVIDER_UNAVAILABLE,
        "ACCOUNT_BANNED" to AuthFailure.ACCOUNT_BANNED,
        "INSTALLATION_ALREADY_REGISTERED" to AuthFailure.INSTALLATION_ALREADY_REGISTERED,
        "INVALID_SIGNUP_TOKEN" to AuthFailure.INVALID_SIGNUP_TOKEN,
        "NICKNAME_FORMAT_INVALID" to AuthFailure.NICKNAME_FORMAT_INVALID,
        "NICKNAME_DUPLICATED" to AuthFailure.NICKNAME_DUPLICATED,
        "NICKNAME_RECENTLY_RELEASED" to AuthFailure.NICKNAME_RECENTLY_RELEASED,
        "BIRTHDATE_INVALID" to AuthFailure.BIRTHDATE_INVALID,
        "BIRTHDATE_UNDERAGE" to AuthFailure.BIRTHDATE_UNDERAGE,
        "GENDER_REQUIRED" to AuthFailure.GENDER_REQUIRED,
        "INTEREST_LIMIT_EXCEEDED" to AuthFailure.INTEREST_LIMIT_EXCEEDED,
        "REQUIRED_AGREEMENT_MISSING" to AuthFailure.REQUIRED_AGREEMENT_MISSING,
        "IMAGE_TOO_LARGE" to AuthFailure.IMAGE_TOO_LARGE,
        "IMAGE_INVALID_TYPE" to AuthFailure.IMAGE_INVALID_TYPE,
        "IMAGE_CORRUPTED" to AuthFailure.IMAGE_CORRUPTED,
        "SESSION_EXPIRED" to AuthFailure.SESSION_EXPIRED,
        "ACCOUNT_LOCKED" to AuthFailure.ACCOUNT_LOCKED,
    )

private const val DEFAULT_TOKEN_TYPE = "Bearer"
