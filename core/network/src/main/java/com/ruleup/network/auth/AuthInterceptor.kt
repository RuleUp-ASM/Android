package com.ruleup.network.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 모든 요청에 `Authorization: Bearer <accessToken>` 헤더를 붙인다.
 * 토큰이 없거나 이미 헤더가 있으면 그대로 통과시킨다.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.header(HEADER) != null) return chain.proceed(request)

        val token = tokenProvider.accessToken() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header(HEADER, "Bearer $token").build(),
        )
    }

    companion object {
        const val HEADER = "Authorization"
    }
}
