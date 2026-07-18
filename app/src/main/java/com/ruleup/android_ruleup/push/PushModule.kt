package com.ruleup.android_ruleup.push

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/** Retrofit 으로 생성한 푸시 토큰 API 를 Hilt 그래프에 제공한다. */
@Module
@InstallIn(SingletonComponent::class)
object PushModule {
    @Provides
    @Singleton
    fun providePushApi(retrofit: Retrofit): PushApi = retrofit.create()
}
