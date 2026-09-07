package com.ruleup.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ruleup.datastore.token.TokenRepositoryImpl
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.e
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val TAG = "TokenStore"

/**
 * Android 전용 `DataStore<Preferences>` 바인딩.
 * `Context.filesDir` 하위에 `token.preferences_pb` 파일을 두고 [TokenRepositoryImpl] 이 소비한다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideTokenDataStore(
        @ApplicationContext context: Context,
        observability: Observability,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler =
                ReplaceFileCorruptionHandler { corruption ->
                    observability.e(TAG, corruption) { "토큰 저장소 손상 — 빈 값으로 복구(재로그인 필요)" }
                    emptyPreferences()
                },
            produceFile = { context.preferencesDataStoreFile("token") },
        )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class TokenRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindTokenRepository(impl: TokenRepositoryImpl): TokenRepository
}
