package com.ruleup.entity.onboarding

import com.ruleup.entity.user.AuthSession

sealed interface OAuthResult {
    data class ExistingUser(
        val session: AuthSession,
    ) : OAuthResult

    data class NewUser(
        val signupToken: String,
        val signupTokenExpireInSeconds: Int,
        val oauthProfile: OAuthProfile,
    ) : OAuthResult
}

data class OAuthProfile(
    val email: String?,
    val profileImageUrlHint: String?,
)
