package com.ruleup.verification.data.di

import com.ruleup.verification.data.repository.HealthTargetStoreImpl
import com.ruleup.verification.data.repository.SignalRepositoryImpl
import com.ruleup.verification.data.repository.UsageTargetStoreImpl
import com.ruleup.verification.data.repository.VerificationRepositoryImpl
import com.ruleup.verification.data.signal.common.SignalCollectorImpl
import com.ruleup.verification.data.signal.geofence.GeofenceRegisterImpl
import com.ruleup.verification.data.sync.DeviceIntroProviderImpl
import com.ruleup.verification.data.sync.EnvelopeMetadataProviderImpl
import com.ruleup.verification.data.sync.ProgressCacheStoreImpl
import com.ruleup.verification.data.sync.SyncPolicyStoreImpl
import com.ruleup.verification.data.sync.SyncScopeProviderImpl
import com.ruleup.verification.data.sync.VerificationSyncSchedulerImpl
import com.ruleup.verification.domain.repository.DeviceIntroProvider
import com.ruleup.verification.domain.repository.EnvelopeMetadataProvider
import com.ruleup.verification.domain.repository.GeofenceRegister
import com.ruleup.verification.domain.repository.HealthTargetStore
import com.ruleup.verification.domain.repository.ProgressCacheStore
import com.ruleup.verification.domain.repository.SignalCollector
import com.ruleup.verification.domain.repository.SignalRepository
import com.ruleup.verification.domain.repository.SyncPolicyStore
import com.ruleup.verification.domain.repository.SyncScheduler
import com.ruleup.verification.domain.repository.SyncScopeProvider
import com.ruleup.verification.domain.repository.UsageTargetStore
import com.ruleup.verification.domain.repository.VerificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VerificationBindingsModule {
    @Binds
    @Singleton
    abstract fun bindVerificationRepository(impl: VerificationRepositoryImpl): VerificationRepository

    @Binds
    @Singleton
    abstract fun bindSignalRepository(impl: SignalRepositoryImpl): SignalRepository

    @Binds
    @Singleton
    abstract fun bindSignalCollector(impl: SignalCollectorImpl): SignalCollector

    @Binds
    @Singleton
    abstract fun bindGeofenceRegister(impl: GeofenceRegisterImpl): GeofenceRegister

    @Binds
    @Singleton
    abstract fun bindProgressCacheStore(impl: ProgressCacheStoreImpl): ProgressCacheStore

    @Binds
    @Singleton
    abstract fun bindSyncScopeProvider(impl: SyncScopeProviderImpl): SyncScopeProvider

    @Binds
    @Singleton
    abstract fun bindEnvelopeMetadataProvider(impl: EnvelopeMetadataProviderImpl): EnvelopeMetadataProvider

    @Binds
    @Singleton
    abstract fun bindDeviceIntroProvider(impl: DeviceIntroProviderImpl): DeviceIntroProvider

    @Binds
    @Singleton
    abstract fun bindSyncPolicyStore(impl: SyncPolicyStoreImpl): SyncPolicyStore

    @Binds
    @Singleton
    abstract fun bindSyncScheduler(impl: VerificationSyncSchedulerImpl): SyncScheduler

    @Binds
    @Singleton
    abstract fun bindHealthTargetStore(impl: HealthTargetStoreImpl): HealthTargetStore

    @Binds
    @Singleton
    abstract fun bindUsageTargetStore(impl: UsageTargetStoreImpl): UsageTargetStore
}
