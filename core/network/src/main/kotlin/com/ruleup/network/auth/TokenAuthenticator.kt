package com.ruleup.network.auth

import com.ruleup.domain.token.TokenRefresher
import com.ruleup.domain.token.TokenRepository
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.i
import com.ruleup.observability.domain.api.w
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

/**
 * 401 을 받은 요청을 저장된 refreshToken 으로 갱신해 자동 재시도하는 OkHttp Authenticator.
 * DI 순환(OkHttpClient → Authenticator → TokenRefresher → AuthApi → Retrofit → OkHttpClient)은 [Lazy] 로 끊는다.
 */
class TokenAuthenticator
    @Inject
    constructor(
        private val tokenRepository: TokenRepository,
        private val tokenRefresher: Lazy<TokenRefresher>,
        private val observability: Observability,
    ) : Authenticator {
        override fun authenticate(
            route: Route?,
            response: Response,
        ): Request? {
            // refresh 자체의 401 까지 갱신하러 들어가면 무한 재귀에 빠진다.
            val requestPath = response.request.url.encodedPath
            if (requestPath.contains(AUTH_REFRESH_PATH)) return null
            if (responseCount(response) >= MAX_ATTEMPTS) return null

            synchronized(this) {
                val failedToken = response.request.header(HEADER_AUTHORIZATION)?.removePrefix(BEARER_PREFIX)
                val current = tokenRepository.cachedAccessToken()
                // 실패한 토큰과 캐시가 다르면 다른 스레드가 이미 갱신을 마친 것이다 — 다시 갱신하지 않는다.
                if (!current.isNullOrBlank() && current != failedToken) {
                    return response.retryWith(current)
                }

                val refreshToken = runBlocking { tokenRepository.getRefreshToken() } ?: return null

                val newToken =
                    try {
                        runBlocking { tokenRefresher.get().refresh(refreshToken) }
                    } catch (e: Exception) {
                        // 네트워크·5xx 는 세션 만료가 아니다 — 토큰을 지우면 멀쩡한 사용자가 로그아웃된다.
                        observability.w(TAG, e) { "토큰 갱신 일시 실패 — 재시도 포기" }
                        return null
                    }

                if (newToken == null) {
                    // 저장된 refreshToken 이 바뀌었으면 다른 경로(콜드스타트 AutoLogin 등)가 이미 회전시킨 것이다 —
                    // 이 요청이 쓴 토큰만 낡았을 뿐 세션은 살아 있으므로 정리하지 않는다.
                    val latestAccess = tokenRepository.cachedAccessToken()
                    val latestRefresh = runBlocking { tokenRepository.getRefreshToken() }
                    if (!latestAccess.isNullOrBlank() && latestRefresh != null && latestRefresh != refreshToken) {
                        return response.retryWith(latestAccess)
                    }
                    // 정리하면 isLoggedIn 이 false 로 전이해 로그인 화면으로 라우팅된다.
                    observability.i(TAG) { "세션 만료 — 로컬 토큰 정리" }
                    runBlocking { tokenRepository.clear() }
                    return null
                }

                runBlocking { tokenRepository.saveTokens(newToken.token, newToken.userId) }
                return response.retryWith(newToken.token.accessToken)
            }
        }

        private fun Response.retryWith(accessToken: String): Request =
            request
                .newBuilder()
                .header(HEADER_AUTHORIZATION, "$BEARER_PREFIX$accessToken")
                .build()

        private fun responseCount(response: Response): Int {
            var count = 1
            var prior = response.priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }

        private companion object {
            const val TAG = "TokenAuthenticator"
            const val AUTH_REFRESH_PATH = "/auth/refresh"
            const val HEADER_AUTHORIZATION = "Authorization"
            const val BEARER_PREFIX = "Bearer "
            const val MAX_ATTEMPTS = 2
        }
    }
