package com.ruleup.data.auth.di

import com.ruleup.data.auth.repository.TokenProviderImpl
import com.ruleup.data.auth.repository.TokenRefresherImpl
import com.ruleup.network.auth.TokenProvider
import com.ruleup.network.auth.TokenRefresher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 네트워크 레이어가 요구하는 토큰 부착/갱신 구현을 바인딩한다. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthTokenModule {
    @Binds
    @Singleton
    abstract fun bindTokenProvider(impl: TokenProviderImpl): TokenProvider

    @Binds
    @Singleton
    abstract fun bindTokenRefresher(impl: TokenRefresherImpl): TokenRefresher
}
