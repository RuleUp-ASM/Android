package com.ruleup.verification.data.di

import com.ruleup.verification.data.api.VerificationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/**
 * Retrofit 으로 생성한 verification API 구현을 Hilt 그래프에 제공한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object VerificationNetworkModule {
    @Provides
    @Singleton
    fun provideVerificationApi(retrofit: Retrofit): VerificationApi = retrofit.create()
}
