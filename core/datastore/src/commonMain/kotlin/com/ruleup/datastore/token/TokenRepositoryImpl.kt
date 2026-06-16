package com.ruleup.datastore.token

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.Token
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TokenRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
) : TokenRepository {
    // refreshToken 존재 여부로 로그인 상태를 반영하는 reactive Flow.
    override val isLoggedIn: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[KEY_REFRESH] != null }

    override suspend fun saveTokens(token: Token) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCESS] = token.accessToken
            prefs[KEY_REFRESH] = token.refreshToken
        }
    }

    override suspend fun getAccessToken(): String? = dataStore.data.map { it[KEY_ACCESS] }.first()

    override suspend fun getRefreshToken(): String? = dataStore.data.map { it[KEY_REFRESH] }.first()

    override suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    companion object {
        private val KEY_ACCESS = stringPreferencesKey("access_token")
        private val KEY_REFRESH = stringPreferencesKey("refresh_token")
    }
}
