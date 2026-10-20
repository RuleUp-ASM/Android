package com.ruleup.android_ruleup.tti

import com.ruleup.domain.device.DeviceInfoProvider
import com.ruleup.observability.domain.api.Observability
import com.ruleup.observability.domain.api.w
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiShooter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** 완성된 TTI 를 구글 시트(Apps Script 웹앱)에 한 행씩 남긴다. URL 이 비어 있으면 보내지 않는다. */
internal class SheetTtiShooter(
    private val url: String,
    private val buildType: String,
    private val client: OkHttpClient,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val observability: Observability,
) : TtiShooter {
    override suspend fun shoot(records: List<TtiRecord>) {
        if (url.isBlank()) return
        val device = deviceInfoProvider.current()
        records.mapNotNull { it.toSheetRow(device, buildType) }.forEach { row ->
            // 던지면 기록이 지워지지 않아 다음 전송 때 Firebase·Amplitude 에도 같은 TTI 가 또 나간다.
            runCatching { post(row) }
                .onFailure { observability.w(TAG, it) { "TTI 시트 전송 실패 — ${row.pageName}" } }
        }
    }

    private suspend fun post(row: TtiSheetRow) =
        withContext(Dispatchers.IO) {
            val request =
                Request
                    .Builder()
                    .url(url)
                    .post(Json.encodeToString(TtiSheetRow.serializer(), row).toRequestBody(JSON))
                    .build()
            client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
            }
        }

    private companion object {
        const val TAG = "TtiSheet"
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
