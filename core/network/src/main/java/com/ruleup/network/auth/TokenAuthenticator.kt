package com.ruleup.network.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * 401(Unauthorized) 응답을 받으면 토큰을 갱신하고 원 요청을 재시도한다.
 *
 * - 동시에 여러 요청이 401 이 나도 [TokenRefresher.refresh] 가 한 번만 일어나도록 동기화한다.
 *   (이미 다른 스레드가 갱신했으면 새 토큰으로 바로 재시도)
 * - refresh 자체가 401 이거나, 재시도 누적이 한도를 넘으면 null 을 반환해 포기한다(재로그인 유도).
 */
class TokenAuthenticator(
    private val tokenProvider: TokenProvider,
    private val tokenRefresher: TokenRefresher,
) : Authenticator {
    private val lock = Any()

    override fun authenticate(
        route: Route?,
        response: Response,
    ): Request? {
        // 공개 엔드포인트(로그인/가입/리프레시 등)의 401 은 갱신 대상이 아니다.
        // 갱신 후에도 계속 401 이면 무한 루프 방지로 포기한다.
        if (AuthInterceptor.isPublicEndpoint(response.request.url.encodedPath)) return null
        if (responseCount(response) >= MAX_RETRY) return null

        val failedToken =
            response.request
                .header(AuthInterceptor.HEADER)
                ?.removePrefix("Bearer ")

        synchronized(lock) {
            // 락 진입 사이 다른 스레드가 이미 갱신했으면 그 토큰을, 아니면 새로 갱신한다.
            val current = tokenProvider.accessToken()?.takeIf { it != failedToken }
            val token = current ?: tokenRefresher.refresh()
            return token?.let { response.request.retryWith(it) }
        }
    }

    private fun Request.retryWith(token: String): Request = newBuilder().header(AuthInterceptor.HEADER, "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    companion object {
        private const val MAX_RETRY = 2
    }
}
