package com.ruleup.onboarding.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/** 기기·설치 식별자 저장소. 토큰 저장소와 구분하는 한정자다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DeviceStore

/**
 * 식별자 전용 `DataStore<Preferences>`.
 *
 * 토큰 저장소와 파일을 나눈다 — 로그아웃은 토큰을 지우지만 기기·설치 식별자는 살아남아야 한다.
 * 한 파일에 두면 `TokenRepository.clear()` 가 식별자까지 날려, 로그아웃할 때마다 새 설치처럼
 * 보이게 된다.
 *
 * 손상 시 빈 값으로 복구한다. 식별자는 재생성 가능하고(재로그인이 필요할 뿐) 여기서 실패하면
 * 로그인 자체가 막히기 때문이다.
 */
@Module
@InstallIn(SingletonComponent::class)
object DeviceStoreModule {
    @Provides
    @Singleton
    @DeviceStore
    fun provideDeviceDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { context.preferencesDataStoreFile("device") },
        )
}
