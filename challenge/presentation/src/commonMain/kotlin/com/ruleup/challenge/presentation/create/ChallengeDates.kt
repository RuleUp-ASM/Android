package com.ruleup.challenge.presentation.create

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 챌린지 기간 계산용 날짜 유틸(멀티플랫폼).
 * 날짜는 ISO(yyyy-MM-dd) 문자열로 주고받는다. 요일은 일요일 시작(1=일 ~ 7=토) 정수로 표현한다.
 */
internal object ChallengeDates {
    private val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    /** ISO 요일(월=1~일=7)을 일요일 시작(일=1~토=7)으로 변환. DayOfWeek.ordinal: 월=0~일=6. */
    private fun LocalDate.sundayBasedDow(): Int = (dayOfWeek.ordinal + 1) % 7 + 1

    @OptIn(ExperimentalTime::class)
    fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    fun todayIso(): String = today().toString()

    /** ISO(yyyy-MM-dd) 문자열을 LocalDate 로 파싱. 실패 시 null. */
    fun parse(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()

    /** [iso] 에 [days]일을 더한 ISO 날짜. 파싱 실패 시 입력을 그대로 돌려준다. */
    fun plusDays(
        iso: String,
        days: Int,
    ): String {
        val date = parse(iso) ?: return iso
        return date.plus(days, DateTimeUnit.DAY).toString()
    }

    /** 종료일 = 시작일 + 기간 - 1 (시작일 포함). */
    fun endDate(
        startIso: String,
        durationDays: Int,
    ): String = plusDays(startIso, durationDays - 1)

    /** [startIso]~[endIso] 사이의 일수(시작일 제외). endDate(s, n) 의 역연산. */
    fun daysBetween(
        startIso: String,
        endIso: String,
    ): Int {
        val start = parse(startIso) ?: return 0
        val end = parse(endIso) ?: return 0
        return start.daysUntil(end)
    }

    /** "06.01 월" 형태의 짧은 표기. */
    fun formatMonthDay(iso: String): String {
        val date = parse(iso) ?: return iso
        val month = date.monthNumber.toString().padStart(2, '0')
        val day = date.dayOfMonth.toString().padStart(2, '0')
        val dayLabel = dayLabels[date.sundayBasedDow() - 1]
        return "$month.$day $dayLabel"
    }

    /** 기간 프리셋 라벨: 7→1주, 14→2주, 28→4주, 90→3개월, 그 외 "n일". */
    fun durationLabel(durationDays: Int): String =
        when (durationDays) {
            7 -> "1주"
            14 -> "2주"
            28 -> "4주"
            90 -> "3개월"
            else -> "${durationDays}일"
        }

    /**
     * 캘린더 한 페이지(6주 = 42칸). 표시 월 밖의 날짜는 inMonth=false.
     * @param month 0-based(0=1월 ~ 11=12월). java.util.Calendar 시절 호출부 계약을 유지한다.
     */
    fun monthCells(
        year: Int,
        month: Int,
    ): List<CalendarCell> {
        val first = LocalDate(year, month + 1, 1)
        // 일요일 시작 그리드의 첫 칸으로 이동
        var cursor = first.plus(-(first.sundayBasedDow() - 1), DateTimeUnit.DAY)
        return List(42) {
            val cell =
                CalendarCell(
                    iso = cursor.toString(),
                    day = cursor.dayOfMonth,
                    inMonth = cursor.monthNumber - 1 == month,
                    dayOfWeek = cursor.sundayBasedDow(),
                )
            cursor = cursor.plus(1, DateTimeUnit.DAY)
            cell
        }
    }
}

internal data class CalendarCell(
    val iso: String,
    val day: Int,
    val inMonth: Boolean,
    // 일요일 시작: 1=일 ~ 7=토
    val dayOfWeek: Int,
)
