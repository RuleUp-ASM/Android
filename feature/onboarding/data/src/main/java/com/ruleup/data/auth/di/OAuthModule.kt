package com.ruleup.data.auth.di

import com.ruleup.data.auth.oauth.GoogleOAuthAuthorizer
import com.ruleup.data.auth.oauth.KakaoOAuthAuthorizer
import com.ruleup.data.auth.oauth.OAuthAuthorizerDispatcher
import com.ruleup.data.auth.oauth.OAuthProviderKey
import com.ruleup.domain.auth.model.OAuthProvider
import com.ruleup.domain.auth.repository.OAuthAuthorizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
interface OAuthModule {
    @Binds
    fun bindOAuthAuthorizer(impl: OAuthAuthorizerDispatcher): OAuthAuthorizer

    @Binds
    @IntoMap
    @OAuthProviderKey(OAuthProvider.KAKAO)
    fun bindKakao(impl: KakaoOAuthAuthorizer): OAuthAuthorizer

    @Binds
    @IntoMap
    @OAuthProviderKey(OAuthProvider.GOOGLE)
    fun bindGoogle(impl: GoogleOAuthAuthorizer): OAuthAuthorizer
}
