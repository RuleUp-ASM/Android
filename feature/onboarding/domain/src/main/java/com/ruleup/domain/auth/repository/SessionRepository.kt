package com.ruleup.domain.auth.repository

import com.ruleup.core.model.Tokens
import com.ruleup.domain.auth.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val isLoggedIn: Flow<Boolean>

    /** 토큰 갱신 실패 등으로 세션이 강제 종료됐음을 알리는 일회성 신호. UI 는 이를 받아 재로그인으로 보낸다. */
    val sessionExpired: Flow<Unit>

    suspend fun saveSession(session: AuthSession)

    suspend fun updateTokens(tokens: Tokens)

    suspend fun currentTokens(): Tokens?

    suspend fun clear()

    /** 세션을 비우고 [sessionExpired] 신호를 발행한다(강제 로그아웃). */
    suspend fun expireSession()
}
