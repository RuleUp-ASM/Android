package com.ruleup.datastore.di

import com.ruleup.datastore.device.DeviceIdentityRepositoryImpl
import com.ruleup.domain.device.DeviceIdentityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeviceIdentityModule {
    @Binds
    @Singleton
    abstract fun bindDeviceIdentityRepository(impl: DeviceIdentityRepositoryImpl): DeviceIdentityRepository
}
