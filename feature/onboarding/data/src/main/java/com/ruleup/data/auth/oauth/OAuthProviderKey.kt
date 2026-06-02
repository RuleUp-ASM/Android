package com.ruleup.data.auth.oauth

import com.ruleup.domain.auth.model.OAuthProvider
import dagger.MapKey

@MapKey
@Retention(AnnotationRetention.RUNTIME)
annotation class OAuthProviderKey(
    val value: OAuthProvider,
)
