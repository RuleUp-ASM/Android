package com.ruleup.data.auth.repository

import com.ruleup.domain.auth.repository.AuthRepository
import com.ruleup.domain.auth.repository.SessionRepository
import com.ruleup.network.auth.TokenRefresher
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Refresh Token 으로 토큰을 갱신하고 새 access token 을 반환한다.
 *
 * [AuthRepository] 는 Retrofit → OkHttpClient → (이 Refresher) 로 순환하므로
 * [dagger.Lazy] 로 주입해 생성 시점 순환을 끊는다.
 * 갱신 실패 시 세션을 비우고(=재로그인) null 을 반환한다.
 */
class TokenRefresherImpl
    @Inject
    constructor(
        private val sessionRepository: SessionRepository,
        private val authRepository: Lazy<AuthRepository>,
    ) : TokenRefresher {
        override fun refresh(): String? =
            runBlocking {
                val refreshToken =
                    sessionRepository.currentTokens()?.refreshToken
                        ?: return@runBlocking null

                runCatching { authRepository.get().refresh(refreshToken) }
                    .onSuccess { sessionRepository.updateTokens(it) }
                    .onFailure { sessionRepository.clear() }
                    .getOrNull()
                    ?.accessToken
            }
    }
