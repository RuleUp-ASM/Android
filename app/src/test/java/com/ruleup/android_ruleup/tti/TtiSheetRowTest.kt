package com.ruleup.android_ruleup.tti

import com.ruleup.domain.device.DeviceInfo
import com.ruleup.tti.domain.Tti
import com.ruleup.tti.domain.TtiRecord
import com.ruleup.tti.domain.TtiSpan
import com.ruleup.tti.domain.TtiTimeline
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** 시트 열과 값이 어긋나면 화면별 TTI 를 엉뚱한 구간으로 읽는다. */
class TtiSheetRowTest {
    @Test
    fun `완성된 기록은 네 구간과 합계를 각자의 열에 KST 시각으로 싣는다`() {
        // 2026-10-20T12:00:00Z = KST 21:00
        val row = record(createdAt = 1_792_497_600_000L).toSheetRow(DEVICE, buildType = "release")

        assertEquals(
            TtiSheetRow(
                recordedAt = "2026-10-20 21:00:00",
                pageName = "home",
                viewCreate = 100,
                backend = 500,
                viewBinding = 150,
                bigPartLoading = 50,
                totalMillis = 800,
                appVersion = "1.0.2 (3)",
                buildType = "release",
                deviceModel = "Google Pixel 8",
                os = "Android 15 (SDK 35)",
            ),
            row,
        )
    }

    @Test
    fun `닫히지 않은 구간이 있으면 시트에 남기지 않는다`() {
        val open = record().let { it.copy(spans = it.spans + (TtiTimeline.BACKEND to TtiSpan(startedAt = 0))) }

        assertNull(open.toSheetRow(DEVICE, buildType = "debug"))
    }

    private fun record(createdAt: Long = 0) =
        TtiRecord(
            tti = Tti("t1"),
            pageName = "home",
            createdAt = createdAt,
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
