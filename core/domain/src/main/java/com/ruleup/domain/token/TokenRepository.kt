package com.ruleup.domain.token

import com.ruleup.entity.user.Token
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    suspend fun saveTokens(token: Token)

    suspend fun getAccessToken(): String?

    suspend fun clear()

    val isLoggedIn: Flow<Boolean>
}
