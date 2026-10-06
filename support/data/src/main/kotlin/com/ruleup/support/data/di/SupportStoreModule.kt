package com.ruleup.support.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ruleup.support.data.repository.SupportPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 문의 확인 기록용 Preferences DataStore 를 [SupportPrefs] qualifier 로 제공한다. */
@Module
@InstallIn(SingletonComponent::class)
object SupportStoreModule {
    @Provides
    @Singleton
    @SupportPrefs
    fun provideSupportDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("support") },
        )
}
