package com.ruleup.challenge.presentation.create

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 챌린지 기간 계산용 날짜 유틸. minSdk 24 에서 java.time 을 쓸 수 없어
 * (디슈가링 미적용) java.util.Calendar 기반으로 처리한다. 날짜는 ISO(yyyy-MM-dd) 문자열로 주고받는다.
 */
internal object ChallengeDates {
    private val dayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

    private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayIso(): String = isoFormat().format(Calendar.getInstance().time)

    fun parse(iso: String): Calendar? =
        runCatching {
            Calendar.getInstance().apply { time = requireNotNull(isoFormat().parse(iso)) }
        }.getOrNull()

    fun format(calendar: Calendar): String = isoFormat().format(calendar.time)

    /** [iso] 에 [days]일을 더한 ISO 날짜. 파싱 실패 시 입력을 그대로 돌려준다. */
    fun plusDays(
        iso: String,
        days: Int,
    ): String {
        val calendar = parse(iso) ?: return iso
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return format(calendar)
    }

    /** 종료일 = 시작일 + 기간 - 1 (시작일 포함). */
    fun endDate(
        startIso: String,
        durationDays: Int,
    ): String = plusDays(startIso, durationDays - 1)

    /**
     * [startIso]~[endIso] 사이의 일수(시작일 제외). 드래그로 잡은 범위의 기간 계산에 쓴다.
     * endDate(s, n) 의 역연산이며, DST 로 인한 23/25시간 일자를 반올림으로 보정한다.
     */
    fun daysBetween(
        startIso: String,
        endIso: String,
    ): Int {
        val start = parse(startIso) ?: return 0
        val end = parse(endIso) ?: return 0
        val dayMillis = 24.0 * 60 * 60 * 1000
        return Math.round((end.timeInMillis - start.timeInMillis) / dayMillis).toInt()
    }

    /** "06.01 월" 형태의 짧은 표기. */
    fun formatMonthDay(iso: String): String {
        val calendar = parse(iso) ?: return iso
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayLabel = dayLabels[calendar.get(Calendar.DAY_OF_WEEK) - 1]
        return String.format(Locale.US, "%02d.%02d %s", month, day, dayLabel)
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

    /** 캘린더 한 페이지(6주 = 42칸). 표시 월 밖의 날짜는 inMonth=false. */
    fun monthCells(
        year: Int,
        month: Int,
    ): List<CalendarCell> {
        val first =
            Calendar.getInstance().apply {
                clear()
                set(year, month, 1)
            }
        // 일요일 시작 그리드의 첫 칸으로 이동
        val cursor = first.clone() as Calendar
        cursor.add(Calendar.DAY_OF_MONTH, -(first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
        return List(42) {
            val cell =
                CalendarCell(
                    iso = format(cursor),
                    day = cursor.get(Calendar.DAY_OF_MONTH),
                    inMonth = cursor.get(Calendar.MONTH) == month,
                    dayOfWeek = cursor.get(Calendar.DAY_OF_WEEK),
                )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
            cell
        }
    }
}

internal data class CalendarCell(
    val iso: String,
    val day: Int,
    val inMonth: Boolean,
    // Calendar.SUNDAY(1) ~ SATURDAY(7)
    val dayOfWeek: Int,
)
