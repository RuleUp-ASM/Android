package com.ruleup.domain.auth.repository

import android.app.Activity
import com.ruleup.domain.auth.model.OAuthAuthorization
import com.ruleup.domain.model.OAuthProvider

interface OAuthAuthorizer {
    suspend fun authorize(
        activity: Activity,
        provider: OAuthProvider,
    ): OAuthAuthorization
}
