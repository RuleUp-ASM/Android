package com.ruleup.android_ruleup.tti

import com.ruleup.domain.device.DeviceInfo
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiSpan
import com.ruleup.tti.domain.TtiTimeline
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 시트 전송이 실패해도 TTI 기록 흐름(Firebase·Amplitude)을 막지 않는다. */
class SheetTtiShooterTest {
    private val requests = mutableListOf<Request>()
    private var status = 200

    private val client =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                requests += chain.request()
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("")
                    .body("ok".toResponseBody("text/plain".toMediaType()))
                    .build()
            }.build()

    @Test
    fun `URL 이 비어 있으면 아무것도 보내지 않는다`() =
        runTest {
            shooter(url = "").shoot(listOf(record("home")))

            assertTrue(requests.isEmpty())
        }

    @Test
    fun `완성된 기록마다 Apps Script 가 읽는 키로 한 건씩 보낸다`() =
        runTest {
            shooter().shoot(listOf(record("home"), record("challenge_detail")))

            assertEquals(listOf("home", "challenge_detail"), requests.map { it.bodyJson()["pageName"] })
            // 키 이름이 바뀌면 시트에 빈 칸이 쌓인다
            assertEquals("500", requests.first().bodyJson()["backend"])
        }

    @Test
    fun `서버가 실패를 돌려줘도 던지지 않는다`() =
        runTest {
            // 던지면 기록이 남아 다음 전송 때 Firebase·Amplitude 로 같은 TTI 가 또 나간다.
            status = 500

            shooter().shoot(listOf(record("home")))

            assertEquals(1, requests.size)
        }

    private fun shooter(url: String = "https://script.google.com/macros/s/test/exec") =
        SheetTtiShooter(
            url = url,
            buildType = "debug",
            client = client,
            deviceInfoProvider = { DEVICE },
            observability = testObservability(),
        )

    private fun Request.bodyJson(): Map<String, String> {
        val text = Buffer().also { body!!.writeTo(it) }.readUtf8()
        return Json.parseToJsonElement(text).jsonObject.mapValues { it.value.jsonPrimitive.content }
    }

    private fun record(page: String) =
        TtiRecord(
            tti = Tti(page),
            pageName = page,
            createdAt = 0,
            spans =
                mapOf(
                    TtiTimeline.VIEW_CREATE to TtiSpan(0, 100),
                    TtiTimeline.BACKEND to TtiSpan(100, 600),
                    TtiTimeline.VIEW_BINDING to TtiSpan(600, 750),
                    TtiTimeline.BIG_PART_LOADING to TtiSpan(750, 800),
                ),
        )

    private companion object {
        val DEVICE =
            DeviceInfo(
                platform = "ANDROID",
                osVersion = "15",
                sdkInt = 35,
                deviceModel = "Pixel 8",
                manufacturer = "Google",
                lowRam = false,
                versionName = "1.0.2",
                versionCode = 3,
            )
    }
}
