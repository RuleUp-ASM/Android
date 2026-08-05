package com.ruleup.onboarding.data.device

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.onboarding.data.di.DeviceStore
import com.ruleup.onboarding.domain.auth.entity.DeviceIdentity
import com.ruleup.onboarding.domain.auth.repository.DeviceIdentityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DeviceIdentityRepository] 구현.
 *
 * `deviceId` 는 `ANDROID_ID` 를 쓴다. Android 8+ 에서 (앱 서명키 + 사용자 + 기기) 단위라
 * **앱 재설치에는 살아남고 기기 교체·초기화에는 바뀐다** — 단일 활성 기기 정책이 원하는 성질과
 * 정확히 맞는다. 듀얼앱·멀티유저 프로필에서 값이 갈리는 것도 "다른 사용 맥락 = 다른 기기"라 의도에
 * 부합한다.
 *
 * `installationId` 는 앱이 만든 UUID 다. 정의상 재설치하면 새 값이어야 하므로 백업에서 제외한다
 * (`backup_rules.xml` · `data_extraction_rules.xml`). 복원되면 동일 설치 다계정 차단이 헛돈다.
 */
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

        /**
         * 읽을 수 없거나 알려진 불량값이면 null. 그 경우 호출부가 UUID 폴백으로 내려간다 —
         * 폴백은 재설치에 살아남지 못해 세션이 한 번 끊기지만, ANDROID_ID 를 못 읽는 예외적인
         * 기기에 한정된다.
         */
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
            /** 일부 저가 기기에 대량 탑재된 것으로 알려진 중복 ANDROID_ID. 기기 식별에 쓸 수 없다. */
            const val KNOWN_BAD_ANDROID_ID = "9774d56d682e549c"

            val KEY_FALLBACK_DEVICE_ID = stringPreferencesKey("fallbackDeviceId")
            val KEY_INSTALLATION_ID = stringPreferencesKey("installationId")
        }
    }
