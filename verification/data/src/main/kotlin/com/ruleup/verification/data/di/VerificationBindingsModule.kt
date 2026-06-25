package com.ruleup.verification.data.di

import com.ruleup.verification.data.repository.HealthTargetStoreImpl
import com.ruleup.verification.data.repository.SignalRepositoryImpl
import com.ruleup.verification.data.repository.UsageTargetStoreImpl
import com.ruleup.verification.data.repository.VerificationRepositoryImpl
import com.ruleup.verification.data.signal.GeofenceRegistrarImpl
import com.ruleup.verification.data.signal.SignalCollectorImpl
import com.ruleup.verification.data.sync.ProgressCacheStoreImpl
import com.ruleup.verification.data.sync.SyncScopeProviderImpl
import com.ruleup.verification.data.sync.VerificationSyncSchedulerImpl
import com.ruleup.verification.domain.port.GeofenceRegistrar
import com.ruleup.verification.domain.port.HealthTargetStore
import com.ruleup.verification.domain.port.ProgressCacheStore
import com.ruleup.verification.domain.port.SignalCollector
import com.ruleup.verification.domain.port.SignalRepository
import com.ruleup.verification.domain.port.SyncScheduler
import com.ruleup.verification.domain.port.SyncScopeProvider
import com.ruleup.verification.domain.port.UsageTargetStore
import com.ruleup.verification.domain.port.VerificationRepository
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
    abstract fun bindGeofenceRegistrar(impl: GeofenceRegistrarImpl): GeofenceRegistrar

    @Binds
    @Singleton
    abstract fun bindProgressCacheStore(impl: ProgressCacheStoreImpl): ProgressCacheStore

    @Binds
    @Singleton
    abstract fun bindSyncScopeProvider(impl: SyncScopeProviderImpl): SyncScopeProvider

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
