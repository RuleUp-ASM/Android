package com.ruleup.data.auth.oauth

import com.ruleup.domain.auth.model.OAuthAuthorization
import com.ruleup.domain.auth.model.OAuthProvider
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import javax.inject.Inject

class OAuthAuthorizerDispatcher
    @Inject
    constructor(
        private val authorizers: Map<OAuthProvider, @JvmSuppressWildcards OAuthAuthorizer>,
    ) : OAuthAuthorizer {
        override suspend fun authorize(provider: OAuthProvider): OAuthAuthorization =
            (authorizers[provider] ?: error("미지원 provider: $provider")).authorize(provider)
    }
