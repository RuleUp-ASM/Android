package com.ruleup.onboarding.presentation.onboarding

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 생일 입력 ↔ 달력 값 변환(#593). 달력은 UTC 자정 millis 로 주고받는다 — 기기 시간대로 바꾸면
 * KST 에서는 달력이 하루 전으로 열리고, UTC 보다 느린 시간대에서는 고른 날의 하루 전이 입력된다.
 */
class BirthDatePickerMillisTest {
    private val original = TimeZone.getDefault()

    @AfterTest
    fun restoreZone() = TimeZone.setDefault(original)

    @Test
    fun `입력한 생일은 달력에 그날 UTC 자정으로 넘어간다`() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))

        val millis = "19990315".birthDigitsToPickerMillis()

        assertEquals(
            LocalDate
                .of(1999, 3, 15)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
            millis,
        )
    }

    @Test
    fun `달력에서 고른 날은 기기 시간대와 상관없이 그날로 입력된다`() {
        val picked =
            LocalDate
                .of(1999, 3, 15)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()

        for (zone in listOf("Asia/Seoul", "America/Los_Angeles", "UTC")) {
            TimeZone.setDefault(TimeZone.getTimeZone(zone))
            assertEquals("19990315", picked.pickerMillisToBirthDigits(), zone)
        }
    }

    @Test
    fun `여덟 자리가 아니거나 없는 날짜면 달력 기본값을 쓰게 null 을 준다`() {
        assertNull("199903".birthDigitsToPickerMillis())
        assertNull("19990231".birthDigitsToPickerMillis())
    }
}
