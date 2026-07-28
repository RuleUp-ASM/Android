package com.ruleup.datastore.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TokenRepositoryImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : TokenRepository {
        // 인터셉터의 동기 조회용 accessToken 스냅샷. 저장/조회/삭제 시 갱신한다.
        @Volatile
        private var cachedAccess: String? = null

        // refreshToken 존재 여부로 로그인 상태를 반영하는 reactive Flow.
        override val isLoggedIn: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[KEY_REFRESH] != null }

        // userId 자체를 관찰한다. isLoggedIn 은 saveTokens 시점에 true 가 되지만 userId 는
        // 그 다음 edit 에서 써지므로, 사용자 귀속이 필요한 쪽은 이 Flow 를 봐야 한다.
        override val userId: Flow<String?> =
            dataStore.data.map { prefs -> prefs[KEY_USER_ID] }

        override suspend fun saveTokens(token: Token) {
            cachedAccess = token.accessToken
            dataStore.edit { prefs ->
                prefs[KEY_ACCESS] = token.accessToken
                prefs[KEY_REFRESH] = token.refreshToken
            }
        }

        override suspend fun getAccessToken(): String? =
            dataStore.data
                .map { it[KEY_ACCESS] }
                .first()
                .also { cachedAccess = it }

        override fun cachedAccessToken(): String? = cachedAccess

        override suspend fun getRefreshToken(): String? = dataStore.data.map { it[KEY_REFRESH] }.first()

        override suspend fun saveUserId(userId: String) {
            dataStore.edit { prefs -> prefs[KEY_USER_ID] = userId }
        }

        override suspend fun getUserId(): String? = dataStore.data.map { it[KEY_USER_ID] }.first()

        override suspend fun clear() {
            cachedAccess = null
            dataStore.edit { it.clear() }
        }

        companion object {
            private val KEY_ACCESS = stringPreferencesKey("access_token")
            private val KEY_REFRESH = stringPreferencesKey("refresh_token")
            private val KEY_USER_ID = stringPreferencesKey("user_id")
        }
    }
