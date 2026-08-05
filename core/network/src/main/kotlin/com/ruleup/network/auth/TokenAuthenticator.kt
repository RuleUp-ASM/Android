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
 * accessToken 만료로 401 을 받은 요청을, 저장된 refreshToken 으로 갱신 후 자동 재시도하는 OkHttp Authenticator.
 *
 * 동작:
 * 1. `/auth/refresh` 자체의 401 이거나 이미 한 번 재시도한 요청이면 포기(무한루프 방지).
 * 2. 다른 스레드가 이미 토큰을 갱신했으면(캐시된 accessToken 이 실패 토큰과 다름) 그 토큰으로 재시도.
 * 3. refreshToken 으로 [TokenRefresher] 재발급 → 성공 시 저장 후 새 accessToken 으로 재시도.
 * 4. 세션 만료([TokenRefresher.refresh] 가 null) → 로컬 토큰 정리(→ isLoggedIn=false → 로그인 라우팅) 후 포기.
 * 5. 일시 오류(예외) → 세션은 유지하고 재시도만 포기.
 *
 * DI 순환(OkHttpClient → Authenticator → TokenRefresher → AuthApi → Retrofit → OkHttpClient)은
 * [Lazy] 주입으로 끊는다.
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
            // refresh 요청 자체가 401 이면 재귀/데드락 방지 위해 즉시 포기(세션 만료로 이어진다).
            val requestPath = response.request.url.encodedPath
            if (requestPath.contains(AUTH_REFRESH_PATH)) return null
            // 재시도한 요청이 또 401 이면 포기.
            if (responseCount(response) >= MAX_ATTEMPTS) return null

            synchronized(this) {
                val failedToken = response.request.header(HEADER_AUTHORIZATION)?.removePrefix(BEARER_PREFIX)
                val current = tokenRepository.cachedAccessToken()
                // 동시 401 중, 다른 스레드가 이미 갱신을 마쳤으면 갱신된 토큰으로 바로 재시도한다.
                if (!current.isNullOrBlank() && current != failedToken) {
                    return response.retryWith(current)
                }

                val refreshToken = runBlocking { tokenRepository.getRefreshToken() } ?: return null

                val newToken =
                    try {
                        runBlocking { tokenRefresher.get().refresh(refreshToken) }
                    } catch (e: Exception) {
                        // 일시 오류(네트워크·5xx 등): 세션 유지, 재시도만 포기.
                        observability.w(TAG, e) { "토큰 갱신 일시 실패 — 재시도 포기" }
                        return null
                    }

                if (newToken == null) {
                    // 동시 갱신 레이스: 다른 경로(예: 콜드스타트 AutoLogin)가 이미 refreshToken 을 회전시켜
                    // 성공했다면, 이 요청이 쓴 refreshToken 은 낡아 401 이 된다. 저장된 refreshToken 이
                    // 바뀌었으면 세션은 유효하므로 정리하지 않고 최신 accessToken 으로 재시도한다.
                    val latestAccess = tokenRepository.cachedAccessToken()
                    val latestRefresh = runBlocking { tokenRepository.getRefreshToken() }
                    if (!latestAccess.isNullOrBlank() && latestRefresh != null && latestRefresh != refreshToken) {
                        return response.retryWith(latestAccess)
                    }
                    // 세션 만료: 토큰 정리 → isLoggedIn Flow 가 false 로 전이 → 로그인 화면으로 라우팅.
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
