package com.ruleup.profile.data.di

import com.ruleup.profile.data.api.MyPageApi
import com.ruleup.profile.data.api.ProfileApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

/** Retrofit 으로 생성한 마이 탭 API 구현을 Hilt 그래프에 제공한다. */
@Module
@InstallIn(SingletonComponent::class)
object ProfileNetworkModule {
    @Provides
    @Singleton
    fun provideMyPageApi(retrofit: Retrofit): MyPageApi = retrofit.create()

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create()
}
