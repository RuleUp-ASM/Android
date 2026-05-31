package com.ruleup.core.datastore.repository

import com.ruleup.core.model.Tokens
import kotlinx.coroutines.flow.Flow

interface AuthTokenDataStore {
    val authToken: Flow<Tokens?>

    val isLoggedIn: Flow<Boolean>

    suspend fun getAuthToken(): Tokens?

    suspend fun updateToken(token: Tokens)

    suspend fun clear()
}
