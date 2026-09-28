package com.ruleup.domain.token

import com.ruleup.domain.entity.user.Token
import kotlinx.coroutines.flow.Flow

interface TokenRepository {
    /** 로그인·가입 완료. */
    suspend fun saveSession(
        token: Token,
        userId: String,
    )

    /** 토큰을 회전시킨다. */
    suspend fun saveTokens(
        token: Token,
        userId: String? = null,
    )

    suspend fun getAccessToken(): String?

    /** 마지막으로 알려진 accessToken 의 인메모리 스냅샷(동기). */
    fun cachedAccessToken(): String?

    suspend fun getRefreshToken(): String?

    /** 저장된 내 userId. */
    suspend fun getUserId(): String?

    /** 저장된 userId 를 reactive 로 관찰한다. */
    val userId: Flow<String?>

    suspend fun clear()

    /** 이 기기에서 한 번이라도 로그인에 성공한 적이 있는지. */
    suspend fun hasEverLoggedIn(): Boolean

    val isLoggedIn: Flow<Boolean>
}
