package com.ruleup.support.data.di

import com.ruleup.support.data.repository.DeviceContextProviderImpl
import com.ruleup.support.data.repository.InquiryRepositoryImpl
import com.ruleup.support.domain.repository.DeviceContextProvider
import com.ruleup.support.domain.repository.InquiryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupportRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindInquiryRepository(impl: InquiryRepositoryImpl): InquiryRepository

    @Binds
    @Singleton
    abstract fun bindDeviceContextProvider(impl: DeviceContextProviderImpl): DeviceContextProvider
}
