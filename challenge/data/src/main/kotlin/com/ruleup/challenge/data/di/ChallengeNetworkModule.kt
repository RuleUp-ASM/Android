package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.api.ChallengeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/**
 * Retrofit 으로 생성한 challenge API 구현을 Hilt 그래프에 제공한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object ChallengeNetworkModule {
    @Provides
    @Singleton
    fun provideChallengeApi(retrofit: Retrofit): ChallengeApi = retrofit.create()
}
