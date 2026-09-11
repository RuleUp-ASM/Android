package com.ruleup.support.data.di

import com.ruleup.support.data.api.InquiryApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupportNetworkModule {
    @Provides
    @Singleton
    fun provideInquiryApi(retrofit: Retrofit): InquiryApi = retrofit.create()
}
