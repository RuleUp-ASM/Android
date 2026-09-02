package com.ruleup.android_ruleup.acceptance

import com.ruleup.android_ruleup.BuildConfig
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

/**
 * 인수 테스트 공통 준비.
 *
 * 이 층은 **실서버 상태를 바꾸므로** 기본 CI 에서 돌지 않는다. `RULEUP_ACCEPTANCE=1` 일 때만
 * 돌고, 그 외에는 실패가 아니라 **건너뜀**이다 — 리포트에 "건너뜀"으로 남아야 존재가 드러난다.
 * 실패로 두면 사람들이 무시하는 법을 배우고, 아예 빼면 있다는 걸 아무도 모른다.
 */
object AcceptanceGate {
    private const val ENABLED = "RULEUP_ACCEPTANCE"
    private const val SECRET = "DEV_TOKEN_SECRET"
    private const val BASE_URL = "RULEUP_ACCEPTANCE_BASE_URL"

    /** 켜지 않았으면 건너뛴다. 모든 인수 테스트의 첫 줄이다. */
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
            // 앱의 NetworkModule 과 같은 설정이다 — 여기서만 관대하면 앱에서 깨지는 응답이 통과한다.
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

    /**
     * 인증 헤더가 붙은 Retrofit. 앱의 `*Api` 인터페이스를 그대로 넘겨 쓴다 —
     * 테스트용 DTO 를 따로 만들면 "테스트에서만 맞는" 계약을 검증하게 된다.
     */
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

    /**
     * 온보딩을 마친 **새 테스트 계정**을 만들어 토큰을 받는다.
     *
     * 계정을 재사용하지 않는 이유 — 앞선 실행이 남긴 참여 이력·재입장 대기가 다음 실행의 전제를
     * 깨뜨린다. 매번 새로 만들면 테스트끼리 순서에 기대지 않는다.
     *
     * 서버는 시크릿이 틀리거나 prod 프로필이면 **404 로 존재를 숨긴다** — 401 을 내리면 경로가
     * 있다는 사실이 새어나가기 때문이다. 그래서 404 는 "미배포"와 "시크릿 불일치" 둘 다일 수 있다.
     */
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
                .url(baseUrl() + "api/v1/dev/tokens")
                .header("X-Dev-Secret", System.getenv(SECRET).orEmpty())
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

        client().newCall(request).execute().use { response ->
            check(response.code != 404) {
                "개발용 토큰 경로가 404 다. 시크릿이 다르거나 이 환경에 배포되지 않았다 — 서버가 둘을 구분해 주지 않는다."
            }
            check(response.isSuccessful) { "개발용 토큰 발급 실패: ${response.code} ${response.body?.string()}" }
            return json.decodeFromString(DevToken.serializer(), response.body!!.string())
        }
    }

    private fun client(auth: ((Request.Builder) -> Request.Builder)? = null): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .apply {
                auth?.let { attach ->
                    addInterceptor { chain -> chain.proceed(attach(chain.request().newBuilder()).build()) }
                }
            }.build()
}

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
