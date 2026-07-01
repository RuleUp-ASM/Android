package com.ruleup.verification.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ruleup.verification.data.settings.VerificationPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 자동인증 전용 Preferences DataStore 를 [VerificationPrefs] qualifier 로 제공한다.
 * core:datastore 의 token DataStore<Preferences> 와 별도 파일(`verification`)·별도 바인딩이다.
 */
@Module
@InstallIn(SingletonComponent::class)
object VerificationSettingsModule {
    @Provides
    @Singleton
    @VerificationPrefs
    fun provideVerificationDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("verification") },
        )
}
