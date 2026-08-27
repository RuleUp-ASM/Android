package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.api.ChallengeApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChallengeNetworkModule {
    @Provides
    @Singleton
    fun provideChallengeApi(retrofit: Retrofit): ChallengeApi = retrofit.create()
}
