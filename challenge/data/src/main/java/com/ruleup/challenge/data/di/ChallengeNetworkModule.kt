package com.ruleup.challenge.data.di

import com.ruleup.challenge.data.api.ChallengeApi
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import retrofit2.Retrofit

@ContributesTo(AppScope::class)
interface ChallengeNetworkModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideChallengeApiService(retrofit: Retrofit): ChallengeApi = retrofit.create(ChallengeApi::class.java)
}
