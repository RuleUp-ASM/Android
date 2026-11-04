package com.ruleup.remoteconfig.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ruleup.remoteconfig.domain.RemoteConfig
import com.ruleup.remoteconfig.domain.RemoteConfigMetrics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class RemoteConfigStore

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {
    @Binds
    abstract fun remoteConfig(implementation: AmplitudeRemoteConfig): RemoteConfig

    @Binds
    abstract fun remoteConfigMetrics(implementation: AmplitudeRemoteConfig): RemoteConfigMetrics

    companion object {
        @Provides
        @Singleton
        @RemoteConfigStore
        fun store(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> =
            PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { context.preferencesDataStoreFile("remote_config") },
            )
    }
}
