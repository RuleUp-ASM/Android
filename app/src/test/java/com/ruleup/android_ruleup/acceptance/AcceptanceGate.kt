package com.ruleup.android_ruleup.acceptance

import com.ruleup.android_ruleup.BuildConfig
import com.ruleup.network.di.ErrorBodyInterceptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assume.assumeTrue
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** 인수 테스트 공통 준비. */
object AcceptanceGate {
    private const val ENABLED = "RULEUP_ACCEPTANCE"
    private const val SECRET = "DEV_TOKEN_SECRET"
    private const val BASE_URL = "RULEUP_ACCEPTANCE_BASE_URL"

    /** 켜지 않았으면 건너뛴다. */
    fun require() {
        assumeTrue("인수 테스트는 $ENABLED=1 일 때만 돈다", System.getenv(ENABLED) == "1")
        assumeTrue("$SECRET 이 없으면 개발용 토큰을 받을 수 없다", !System.getenv(SECRET).isNullOrBlank())
    }

    fun baseUrl(): String {
        val raw = System.getenv(BASE_URL) ?: BuildConfig.BASE_URL
        require(raw.isNotBlank()) { "$BASE_URL 도 BuildConfig.BASE_URL 도 비어 있다" }
        return if (raw.endsWith("/")) raw else "$raw/"
    }

    private val json =
        Json {

            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    /** 인증 헤더가 붙은 Retrofit. */
    fun <T> api(
        service: Class<T>,
        accessToken: String,
    ): T =
        Retrofit
            .Builder()
            .baseUrl(baseUrl())
            .client(client { it.header("Authorization", "Bearer $accessToken") })
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(service)

    /** 온보딩을 마친 새 테스트 계정을 만들어 토큰을 받는다. */
    fun issueToken(
        tier: String? = null,
        status: String? = null,
    ): DevToken {
        val body =
            buildString {
                append("{")
                tier?.let { append("\"tier\":\"$it\",") }
                status?.let { append("\"status\":\"$it\",") }
                append("\"agreements\":true")
                append("}")
            }
        val request =
            Request
                .Builder()
                .url(baseUrl() + "v1/dev/tokens")
                .header("X-Dev-Secret", System.getenv(SECRET).orEmpty())
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

        client().newCall(request).execute().use { response ->
            check(response.code != 404) {
                "개발용 토큰 경로가 404 다. 시크릿이 다르거나 이 환경에 배포되지 않았다 — 서버가 둘을 구분해 주지 않는다."
            }
            check(response.isSuccessful) { "개발용 토큰 발급 실패: ${response.code} ${response.body?.string()}" }

            val envelope = json.decodeFromString(DevTokenEnvelope.serializer(), response.body!!.string())
            return checkNotNull(envelope.data) { "개발용 토큰 응답에 data 가 없다" }
        }
    }

    private fun client(auth: ((Request.Builder) -> Request.Builder)? = null): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(ErrorBodyInterceptor())
            .apply {
                auth?.let { attach ->
                    addInterceptor { chain -> chain.proceed(attach(chain.request().newBuilder()).build()) }
                }
            }.build()
}

/** `{success, data, error}` 봉투. */
@Serializable
data class DevTokenEnvelope(
    @SerialName("data") val data: DevToken? = null,
)

@Serializable
data class DevToken(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("created") val created: Boolean = false,
    @SerialName("user") val user: DevUser,
)

@Serializable
data class DevUser(
    @SerialName("userId") val userId: String,
    @SerialName("nickname") val nickname: String,
    @SerialName("status") val status: String? = null,
    @SerialName("tier") val tier: String? = null,
    @SerialName("displayTier") val displayTier: String? = null,
    @SerialName("score") val score: Int? = null,
)
