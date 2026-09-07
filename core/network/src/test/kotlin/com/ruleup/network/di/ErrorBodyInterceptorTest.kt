package com.ruleup.network.di

import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 4xx 업무 오류를 Retrofit 이 읽게 만드는 인터셉터(#417).
 *
 * 이게 깨지면 **사용자에게 보이는 문구가 통째로 바뀐다** — 서버가 준 "필수 약관은 철회할 수
 * 없어요" 대신 `HTTP 400` 이 뜨고, 사유별로 갈려야 할 화면이 일반 오류로 접힌다.
 *
 * 반대로 아무 응답이나 200 으로 바꾸면 게이트웨이 HTML 5xx 에서 역직렬화 예외가 나서 원인이
 * 더 안 보이게 된다. 그래서 **우리 봉투일 때만** 바꾼다.
 */
class ErrorBodyInterceptorTest {
    @Test
    fun `업무 오류 본문은 상태 코드를 바꿔 Retrofit 이 읽게 한다`() {
        val code = intercept(status = 400, body = """{"success":false,"error":{"code":"AGREEMENT_REVOKE_FORBIDDEN"}}""")

        assertEquals(200, code)
    }

    @Test
    fun `성공 응답은 손대지 않는다`() {
        val code = intercept(status = 200, body = """{"success":true,"data":{}}""")

        assertEquals(200, code)
    }

    @Test
    fun `우리 봉투가 아닌 JSON 은 그대로 둔다`() {
        // 봉투가 아니면 BaseResponse 로 파싱되지 않는다 — 바꿔 봐야 역직렬화 예외만 앞당긴다.
        val code = intercept(status = 502, body = """{"message":"Bad Gateway"}""")

        assertEquals(502, code)
    }

    @Test
    fun `JSON 이 아닌 오류 본문은 그대로 둔다`() {
        // 게이트웨이는 HTML 을 뱉는다. 200 으로 바꾸면 원인이 더 안 보이게 된다.
        val code = intercept(status = 503, body = "<html>maintenance</html>", contentType = "text/html")

        assertEquals(503, code)
    }

    @Test
    fun `본문을 들여다봐도 Retrofit 이 다시 읽을 수 있어야 한다`() {
        // peek 이 아니라 소비해 버리면 본문이 비어 파싱이 깨진다.
        val payload = """{"success":false,"error":{"code":"JOIN_BLOCKED"}}"""
        val response = ErrorBodyInterceptor().intercept(chain(response(400, payload)))

        assertEquals(payload, response.body?.string())
    }

    private fun intercept(
        status: Int,
        body: String,
        contentType: String = "application/json",
    ): Int = ErrorBodyInterceptor().intercept(chain(response(status, body, contentType))).code

    private fun response(
        status: Int,
        body: String,
        contentType: String = "application/json",
    ): Response =
        Response
            .Builder()
            .request(Request.Builder().url("https://example.test/v1/x").build())
            .protocol(Protocol.HTTP_1_1)
            .code(status)
            .message("")
            .body(body.toResponseBody(contentType.toMediaType()))
            .build()

    /** 응답 하나만 돌려주는 최소 Chain. 나머지는 호출되면 그 자체가 의도치 않은 사용이다. */
    private fun chain(response: Response): Interceptor.Chain =
        object : Interceptor.Chain {
            override fun request(): Request = response.request

            override fun proceed(request: Request): Response = response

            override fun connection(): Connection? = null

            override fun call(): Call = throw NotImplementedError()

            override fun connectTimeoutMillis(): Int = 0

            override fun withConnectTimeout(
                timeout: Int,
                unit: java.util.concurrent.TimeUnit,
            ): Interceptor.Chain = this

            override fun readTimeoutMillis(): Int = 0

            override fun withReadTimeout(
                timeout: Int,
                unit: java.util.concurrent.TimeUnit,
            ): Interceptor.Chain = this

            override fun writeTimeoutMillis(): Int = 0

            override fun withWriteTimeout(
                timeout: Int,
                unit: java.util.concurrent.TimeUnit,
            ): Interceptor.Chain = this
        }
}
