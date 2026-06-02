package com.ruleup.data.auth.repository

import com.ruleup.core.datastore.repository.AuthTokenDataStore
import com.ruleup.core.model.Tokens
import com.ruleup.domain.auth.model.AuthSession
import com.ruleup.domain.auth.repository.SessionRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

class SessionRepositoryImpl
    @Inject
    constructor(
        private val dataStore: AuthTokenDataStore,
    ) : SessionRepository {
        override val isLoggedIn: Flow<Boolean>
            get() = dataStore.isLoggedIn

        // 구독자가 잠시 없어도(백그라운드 등) 직전 만료 1건은 버퍼에 남겨 복귀 시 전달. emit 이 블로킹되지 않도록 DROP_OLDEST.
        private val _sessionExpired =
            MutableSharedFlow<Unit>(
                extraBufferCapacity = 1,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        override val sessionExpired: Flow<Unit> = _sessionExpired.asSharedFlow()

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

        override suspend fun expireSession() {
            dataStore.clear()
            _sessionExpired.emit(Unit)
        }
    }
