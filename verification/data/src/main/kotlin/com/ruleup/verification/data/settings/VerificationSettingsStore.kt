package com.ruleup.verification.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Qualifier

/** verification 전용 Preferences DataStore 식별 qualifier(core:datastore 의 token DataStore 와 분리). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VerificationPrefs

/**
 * 자동인증 클라이언트 상태 영속(전송 스펙 §0.1·§0.3·§0.7 — 부팅 세션·진단 앵커·서버 정책).
 * 부팅 epoch 앵커가 바뀌면 새 세션 id 를 발급한다 — 서버는 같은 세션 안의 시각 점프로 위조를 본다(§6.4).
 */
class VerificationSettingsStore
    @Inject
    constructor(
        @VerificationPrefs private val dataStore: DataStore<Preferences>,
    ) {
        suspend fun bootSessionId(): String? = dataStore.data.first()[KEY_BOOT_SESSION_ID]

        suspend fun bootEpochAnchor(): Long? = dataStore.data.first()[KEY_BOOT_EPOCH_ANCHOR]

        suspend fun setBootSession(
            sessionId: String,
            bootEpoch: Long,
        ) {
            dataStore.edit {
                it[KEY_BOOT_SESSION_ID] = sessionId
                it[KEY_BOOT_EPOCH_ANCHOR] = bootEpoch
            }
        }

        suspend fun lastSuccessfulFlushAt(): Long? = dataStore.data.first()[KEY_LAST_FLUSH_AT]

        suspend fun setLastSuccessfulFlushAt(at: Long) {
            dataStore.edit { it[KEY_LAST_FLUSH_AT] = at }
        }

        suspend fun lastGeofenceReregisterAt(): Long? = dataStore.data.first()[KEY_LAST_GEOFENCE_REREGISTER_AT]

        suspend fun setLastGeofenceReregisterAt(at: Long) {
            dataStore.edit { it[KEY_LAST_GEOFENCE_REREGISTER_AT] = at }
        }

        /** Phase 0 서버 정책: 주기 flush 간격(초). 미수신이면 null → 호출자가 기본값(1800) 사용. */
        suspend fun flushIntervalSec(): Long? = dataStore.data.first()[KEY_FLUSH_INTERVAL_SEC]

        suspend fun setFlushIntervalSec(sec: Long) {
            dataStore.edit { it[KEY_FLUSH_INTERVAL_SEC] = sec }
        }

        private companion object {
            val KEY_BOOT_SESSION_ID = stringPreferencesKey("boot_session_id")
            val KEY_BOOT_EPOCH_ANCHOR = longPreferencesKey("boot_epoch_anchor")
            val KEY_LAST_FLUSH_AT = longPreferencesKey("last_successful_flush_at")
            val KEY_LAST_GEOFENCE_REREGISTER_AT = longPreferencesKey("last_geofence_reregister_at")
            val KEY_FLUSH_INTERVAL_SEC = longPreferencesKey("flush_interval_sec")
        }
    }
