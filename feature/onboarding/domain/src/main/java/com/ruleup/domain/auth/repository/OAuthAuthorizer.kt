package com.ruleup.domain.auth.repository

import com.ruleup.domain.auth.model.OAuthAuthorization
import com.ruleup.domain.auth.model.OAuthProvider

interface OAuthAuthorizer {
    suspend fun authorize(provider: OAuthProvider): OAuthAuthorization
}
