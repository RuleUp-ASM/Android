package com.ruleup.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * Android 전용 DataStore<Preferences> 바인딩.
 * Context.filesDir 하위에 token.preferences_pb 파일을 두고, commonMain 의 TokenRepositoryImpl 이 소비한다.
 * iOS 추가 시 iosMain 에 동일한 바인딩(NSDocumentDirectory 경로 기반)을 actual 로 제공하면 된다.
 */
@ContributesTo(AppScope::class)
interface DataStoreModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideTokenDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("token") },
        )
}
