package com.ruleup.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * iOS 전용 DataStore<Preferences> 바인딩. NSDocumentDirectory 하위에 token.preferences_pb 파일을 둔다.
 * [com.ruleup.datastore.di.DataStoreModule](androidMain, Context 기반)의 iOS 대응.
 */
@ContributesTo(AppScope::class)
interface DataStoreModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideTokenDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = { "${documentDirectory()}/token.preferences_pb".toPath() },
        )
}

private fun documentDirectory(): String {
    val documents =
        NSFileManager.defaultManager
            .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            .first() as NSURL
    return requireNotNull(documents.path) { "문서 디렉터리를 찾을 수 없습니다" }
}
