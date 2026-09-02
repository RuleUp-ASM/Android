package com.ruleup.domain.test

import com.ruleup.domain.entity.user.Token
import com.ruleup.domain.token.TokenRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 세션 상태를 메모리에 들고 있는 [TokenRepository].
 *
 * 실제 구현처럼 저장 여부를 흐름으로도 흘려보낸다 — 정적 값으로 두면 세션 종료 전이를 관찰하는
 * 코드를 테스트할 수 없다. 여러 feature 의 테스트가 쓰므로 `core:domain` testFixtures 에 둔다.
 */
class FakeTokenRepository(
    private var refreshToken: String? = null,
    private var storedUserId: String? = null,
    private val everLoggedIn: Boolean = false,
) : TokenRepository {
    private val loggedIn = MutableStateFlow(refreshToken != null)
    private val userIdFlow = MutableStateFlow(storedUserId)

    var savedToken: Token? = null
        private set
    var cleared: Boolean = false
        private set

    override val isLoggedIn: Flow<Boolean> = loggedIn.asStateFlow()

    override val userId: Flow<String?> = userIdFlow.asStateFlow()

    override suspend fun saveSession(
        token: Token,
        userId: String,
    ) {
        savedToken = token
        refreshToken = token.refreshToken
        storedUserId = userId
        userIdFlow.value = userId
        loggedIn.value = true
    }

    override suspend fun saveTokens(
        token: Token,
        userId: String?,
    ) {
        savedToken = token
        refreshToken = token.refreshToken
        // null 이면 기존 값을 그대로 둔다 — 덮어 비우면 사용자 귀속이 끊긴다(실제 구현과 같은 규칙).
        userId?.let {
            storedUserId = it
            userIdFlow.value = it
        }
        loggedIn.value = true
    }

    override suspend fun getAccessToken(): String? = savedToken?.accessToken

    override fun cachedAccessToken(): String? = savedToken?.accessToken

    override suspend fun getRefreshToken(): String? = refreshToken

    override suspend fun getUserId(): String? = storedUserId

    override suspend fun clear() {
        cleared = true
        refreshToken = null
        storedUserId = null
        userIdFlow.value = null
        loggedIn.value = false
    }

    override suspend fun hasEverLoggedIn(): Boolean = everLoggedIn || savedToken != null
}
