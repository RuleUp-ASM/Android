package com.ruleup.onboarding.data.intro.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.ruleup.onboarding.data.di.DeviceStore
import com.ruleup.onboarding.domain.intro.repository.WalkthroughRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * 열람 여부를 기기 저장소([DeviceStore])에 둔다 — 토큰 저장소는 로그아웃 때 비워져서,
 * 거기 두면 재로그인마다 워크쓰루가 다시 뜬다.
 *
 * 읽기가 실패하면 "못 봤다"로 떨어진다. 소개를 한 번 더 보는 건 성가신 정도지만, 반대로 잘못
 * 판단하면 첫 사용자가 앱을 아무 설명 없이 만나게 된다.
 */
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
