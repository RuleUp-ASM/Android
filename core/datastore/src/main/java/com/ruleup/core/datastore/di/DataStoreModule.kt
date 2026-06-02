package com.ruleup.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.ruleup.core.datastore.repository.AuthTokenDataStore
import com.ruleup.core.datastore.repository.AuthTokenDataStoreImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val AUTH_PREFERENCES = "auth_preferences"

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = AUTH_PREFERENCES,
)

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreProvidesModule {
    @Provides
    @Singleton
    fun provideAuthPreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.authDataStore
}

@Module
@InstallIn(SingletonComponent::class)
internal fun interface DataStoreBindsModule {
    @Binds
    @Singleton
    fun bindAuthTokenDataStore(impl: AuthTokenDataStoreImpl): AuthTokenDataStore
}
