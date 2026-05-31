package com.ruleup.domain.repository

import com.ruleup.core.model.Tokens
import com.ruleup.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val isLoggedIn: Flow<Boolean>

    suspend fun saveSession(session: AuthSession)

    suspend fun updateTokens(tokens: Tokens)

    suspend fun currentTokens(): Tokens?

    suspend fun clear()
}
