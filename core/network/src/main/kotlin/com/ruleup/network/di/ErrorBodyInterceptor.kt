package com.ruleup.network.di

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 서버가 4xx·5xx 로 내려보낸 **업무 오류 본문을 Retrofit 이 읽게 만든다.**
 *
 * Retrofit 은 비-2xx 에서 본문을 파싱하기 전에 `HttpException` 을 던진다. 그래서
 * `BaseResponse.getOrThrow()` 는 2xx 본문만 보게 되고, 코드별 분기(`JOIN_BLOCKED`,
 * `AGREEMENT_REVOKE_FORBIDDEN` …)가 통째로 죽는다 — 사용자는 서버가 준 한국어 문구 대신
 * `HTTP 400` 을 본다.
 *
 * 그래서 **본문이 우리 봉투인 경우에만** 상태 코드를 200 으로 바꿔 넘긴다. `success:false` 는
 * 그대로 남으므로 `getOrThrow()` 가 `ApiException` 을 던진다 — 실패가 사라지는 게 아니라
 * 표현이 바뀔 뿐이다.
 *
 * 두 가지를 건드리지 않는다.
 * - **401 재발급**: 애플리케이션 인터셉터는 `RetryAndFollowUpInterceptor` 바깥이라, 여기 오는
 *   응답은 이미 `TokenAuthenticator` 가 갱신을 시도한 뒤의 최종 결과다.
 * - **우리 봉투가 아닌 응답**: 게이트웨이가 뱉는 HTML 5xx 같은 것은 그대로 둔다. 바꿔 버리면
 *   `HttpException` 대신 역직렬화 예외가 나서 원인이 더 안 보인다.
 */
class ErrorBodyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        val body = response.body ?: return response
        val contentType = body.contentType()
        if (contentType?.subtype?.contains("json", ignoreCase = true) != true) return response

        // peek 은 본문을 소비하지 않는다 — 원본 스트림은 Retrofit 이 다시 읽어야 한다.
        val peeked = response.peekBody(MAX_PEEK_BYTES).string()
        if (!peeked.looksLikeEnvelope()) return response

        return response
            .newBuilder()
            .code(REWRITTEN_CODE)
            .message(response.message.ifBlank { "OK" })
            .build()
    }

    /**
     * 우리 봉투인가. `success` 키만 본다 — 여기서 완전한 역직렬화를 하면 본문을 두 번 파싱하게 되고,
     * 필드가 하나 늘 때마다 이 판정이 흔들린다.
     */
    private fun String.looksLikeEnvelope(): Boolean = contains("\"success\"")

    private companion object {
        /**
         * 상태 코드를 바꾼 뒤에도 `BaseResponse` 는 `success:false` 를 그대로 들고 있다.
         * 성공으로 오인되지 않는 이유가 그것이다.
         */
        const val REWRITTEN_CODE = 200

        // 에러 본문은 짧다. 큰 본문을 통째로 들여다볼 이유가 없다.
        const val MAX_PEEK_BYTES = 64L * 1024
    }
}
