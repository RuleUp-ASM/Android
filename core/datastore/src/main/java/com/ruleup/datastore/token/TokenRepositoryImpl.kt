package com.ruleup.datastore.token

import android.content.Context
import androidx.core.content.edit
import com.ruleup.domain.token.TokenRepository
import com.ruleup.entity.user.Token
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class TokenRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : TokenRepository {
        private val prefs = context.getSharedPreferences("token", Context.MODE_PRIVATE)
        private val _isLoggedIn = MutableStateFlow(prefs.getString(KEY_REFRESH, null) != null)
        override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

        override suspend fun saveTokens(token: Token) {
            prefs.edit {
                putString(KEY_ACCESS, token.accessToken)
                putString(KEY_REFRESH, token.refreshToken)
            }
            _isLoggedIn.value = true
        }

        override suspend fun getAccessToken(): String? = prefs.getString(KEY_ACCESS, null)

        override suspend fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH, null)

        override suspend fun clear() {
            prefs.edit { clear() }
            _isLoggedIn.value = false
        }

        companion object {
            private const val KEY_ACCESS = "access_token"
            private const val KEY_REFRESH = "refresh_token"
        }
    }
