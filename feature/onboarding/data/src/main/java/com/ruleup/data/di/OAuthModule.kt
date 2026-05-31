package com.ruleup.data.di

import com.ruleup.data.repository.KakaoOAuthAuthorizer
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class OAuthModule {
    @Binds
    abstract fun bindOAuthAuthorizer(impl: KakaoOAuthAuthorizer): OAuthAuthorizer
}
