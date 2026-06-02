package com.ruleup.data.auth.repository

import com.ruleup.domain.auth.repository.SessionRepository
import com.ruleup.network.auth.TokenProvider
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * 저장소(DataStore)에서 access token 을 읽어 제공한다.
 * OkHttp 네트워크 스레드에서 호출되므로 suspend 접근을 [runBlocking] 으로 감싼다.
 */
class TokenProviderImpl
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
    ) : TokenProvider {
        override fun accessToken(): String? = runBlocking { sessionRepository.currentTokens()?.accessToken }
    }
