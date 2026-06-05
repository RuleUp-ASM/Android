package com.ruleup.datastore.token.di

import com.ruleup.datastore.token.TokenRepositoryImpl
import com.ruleup.domain.token.TokenRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface TokenModule {
    @Binds
    @Singleton
    fun provideTokenModule(impl: TokenRepositoryImpl): TokenRepository
}
