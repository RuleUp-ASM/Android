package com.ruleup.onboarding.data.intro.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ruleup.datastore.di.DeviceStore
import com.ruleup.onboarding.domain.intro.repository.WalkthroughRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** 열람 여부를 기기 저장소([DeviceStore])에 둔다 */
class WalkthroughRepositoryImpl
    @Inject
    constructor(
        @DeviceStore private val dataStore: DataStore<Preferences>,
    ) : WalkthroughRepository {
        override suspend fun isSeen(): Boolean = runCatching { dataStore.data.first()[KEY_SEEN] }.getOrNull() ?: false

        override suspend fun markSeen() {
            runCatching { dataStore.edit { it[KEY_SEEN] = true } }
        }

        private companion object {
            val KEY_SEEN = booleanPreferencesKey("walkthrough_seen")
        }
    }
