package com.ruleup.domain.token

import com.ruleup.entity.user.Token
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    suspend fun saveTokens(token: Token)

    suspend fun getAccessToken(): String?

    /**
     * 마지막으로 알려진 accessToken 의 인메모리 스냅샷(동기). 저장/조회/삭제 시 갱신된다.
     * 아직 워밍되지 않았으면 null. OkHttp 인터셉터가 매 요청 DataStore 를 블로킹 조회하지 않도록 쓴다.
     */
    fun cachedAccessToken(): String?

    suspend fun getRefreshToken(): String?

    suspend fun clear()

    val isLoggedIn: Flow<Boolean>
}
