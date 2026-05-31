package com.ruleup.core.datastore.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ruleup.core.datastore.repository.AuthTokenDataStore
import com.ruleup.core.model.Tokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class AuthTokenDataStoreImpl
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : AuthTokenDataStore {
        override val authToken: Flow<Tokens?> =
            dataStore.data.map { prefs -> prefs.toTokens() }

        override val isLoggedIn: Flow<Boolean> =
            dataStore.data.map { prefs -> !prefs[Keys.ACCESS_TOKEN].isNullOrEmpty() }

        override suspend fun getAuthToken(): Tokens? = dataStore.data.first().toTokens()

        override suspend fun updateToken(token: Tokens) {
            dataStore.edit { prefs ->
                prefs[Keys.ACCESS_TOKEN] = token.accessToken
                prefs[Keys.REFRESH_TOKEN] = token.refreshToken
                prefs[Keys.TOKEN_TYPE] = token.tokenType
                prefs[Keys.EXPIRES_IN] = token.expiresInSeconds
            }
        }

        override suspend fun clear() {
            dataStore.edit { it.clear() }
        }

        private fun Preferences.toTokens(): Tokens? {
            val accessToken = this[Keys.ACCESS_TOKEN] ?: return null
            val refreshToken = this[Keys.REFRESH_TOKEN] ?: return null
            return Tokens(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = this[Keys.TOKEN_TYPE] ?: DEFAULT_TOKEN_TYPE,
                expiresInSeconds = this[Keys.EXPIRES_IN] ?: 0,
            )
        }

        private object Keys {
            val ACCESS_TOKEN = stringPreferencesKey("access_token")
            val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
            val TOKEN_TYPE = stringPreferencesKey("token_type")
            val EXPIRES_IN = intPreferencesKey("expires_in_seconds")
        }

        private companion object {
            const val DEFAULT_TOKEN_TYPE = "Bearer"
        }
    }
