package com.ruleup.datastore.device

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.datastore.di.DeviceStore
import com.ruleup.domain.device.DeviceIdentity
import com.ruleup.domain.device.DeviceIdentityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** [DeviceIdentityRepository] 구현. */
@Singleton
class DeviceIdentityRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @DeviceStore private val dataStore: DataStore<Preferences>,
    ) : DeviceIdentityRepository {
        private val mutex = Mutex()

        override suspend fun current(): DeviceIdentity =
            DeviceIdentity(
                deviceId = androidId() ?: readOrCreate(KEY_FALLBACK_DEVICE_ID),
                installationId = readOrCreate(KEY_INSTALLATION_ID),
            )

        /** 읽을 수 없거나 알려진 불량값이면 null */
        private fun androidId(): String? =
            runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }.getOrNull()
                ?.takeIf { it.isNotBlank() && it != KNOWN_BAD_ANDROID_ID }

        private suspend fun readOrCreate(key: Preferences.Key<String>): String =
            mutex.withLock {
                dataStore.data.first()[key]
                    ?: UUID.randomUUID().toString().also { generated ->
                        dataStore.edit { it[key] = generated }
                    }
            }

        private companion object {
            /** 일부 저가 기기에 대량 탑재된 것으로 알려진 중복 ANDROID_ID. */
            const val KNOWN_BAD_ANDROID_ID = "9774d56d682e549c"

            val KEY_FALLBACK_DEVICE_ID = stringPreferencesKey("fallbackDeviceId")
            val KEY_INSTALLATION_ID = stringPreferencesKey("installationId")
        }
    }
