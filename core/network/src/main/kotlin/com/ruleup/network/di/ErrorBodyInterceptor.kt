package com.ruleup.network.di

import okhttp3.Interceptor
import okhttp3.Response

/** 서버가 4xx·5xx 로 내려보낸 업무 오류 본문을 Retrofit 이 읽게 만든다. */
class ErrorBodyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        val body = response.body ?: return response
        val contentType = body.contentType()
        if (contentType?.subtype?.contains("json", ignoreCase = true) != true) return response

        // peek 은 본문을 소비하지 않는다
        val peeked = response.peekBody(MAX_PEEK_BYTES).string()
        if (!peeked.looksLikeEnvelope()) return response

        return response
            .newBuilder()
            .code(REWRITTEN_CODE)
            .message(response.message.ifBlank { "OK" })
            .build()
    }

    /** 우리 봉투인가. */
    private fun String.looksLikeEnvelope(): Boolean = contains("\"success\"")

    private companion object {
        /** 상태 코드를 바꾼 뒤에도 `BaseResponse` 는 `success:false` 를 그대로 들고 있다. */
        const val REWRITTEN_CODE = 200

        // 에러 본문은 짧다.
        const val MAX_PEEK_BYTES = 64L * 1024
    }
}
