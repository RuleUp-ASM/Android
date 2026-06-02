package com.ruleup.data.auth.repository

import com.ruleup.core.datastore.repository.AuthTokenDataStore
import com.ruleup.core.model.Tokens
import com.ruleup.domain.auth.model.AuthSession
import com.ruleup.domain.auth.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SessionRepositoryImpl
    @Inject
    constructor(
        private val dataStore: AuthTokenDataStore,
    ) : SessionRepository {
        override val isLoggedIn: Flow<Boolean>
            get() = dataStore.isLoggedIn

        override suspend fun saveSession(session: AuthSession) {
            dataStore.updateToken(session.tokens)
        }

        override suspend fun updateTokens(tokens: Tokens) {
            dataStore.updateToken(tokens)
        }

        override suspend fun currentTokens(): Tokens? = dataStore.getAuthToken()

        override suspend fun clear() {
            dataStore.clear()
        }
    }
