package com.ruleup.network.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 인증이 필요한 요청에 `Authorization: Bearer <accessToken>` 헤더를 붙인다.
 *
 * 단, 토큰 없이 호출해야 하는 공개 엔드포인트(로그인/가입/리프레시/닉네임확인)에는 붙이지 않는다.
 * 만료된 토큰을 이런 엔드포인트에 붙이면 백엔드 JWT 필터가 로직 실행 전에 401 로 막아버린다.
 */
class AuthInterceptor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isPublicEndpoint(request.url.encodedPath) || request.header(HEADER) != null) {
            return chain.proceed(request)
        }

        val token = tokenProvider.accessToken() ?: return chain.proceed(request)
        return chain.proceed(
            request.newBuilder().header(HEADER, "Bearer $token").build(),
        )
    }

    companion object {
        const val HEADER = "Authorization"

        /** 토큰 없이 호출하는 공개 엔드포인트. Authorization 도, 401 자동 refresh 도 적용하지 않는다. */
        private val PUBLIC_PATHS =
            listOf(
                "auth/oauth",
                "auth/signup",
                "auth/refresh",
                "auth/nickname-availability",
            )

        fun isPublicEndpoint(encodedPath: String): Boolean = PUBLIC_PATHS.any { encodedPath.contains(it) }
    }
}
