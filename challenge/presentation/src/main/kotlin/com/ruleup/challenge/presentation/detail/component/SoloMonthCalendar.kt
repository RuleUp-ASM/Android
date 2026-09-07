package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeCalendar
import com.ruleup.challenge.domain.entity.ChallengeDayStatus
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * 솔로 상세의 월 캘린더 (Figma 1134:1930).
 *
 * **판정 대상일만 색을 갖는다.** 주 3회 방이면 한 달에 12~13칸만 응답에 오고 나머지는 빈 날짜다 —
 * 없는 날을 실패로 칠하면 사용자가 쉬는 날에 실패했다고 읽는다.
 *
 * 유예 창(`CHECKING`)을 실패와 다른 색으로 그린다. 그 구간은 아직 뒤집힐 수 있고(인증 정책
 * §2.1), 확정 실패와 같아 보이면 이의를 낼 수 있는데도 포기하게 된다.
 *
 * 그룹 방에서는 그리지 않는다 — 같은 자리를 랭킹·피드가 쓴다.
 */
@Composable
internal fun SoloMonthCalendar(
    month: String,
    calendar: ChallengeCalendar?,
    isLoading: Boolean,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MonthHeader(month = month, onPrevMonth = onPrevMonth, onNextMonth = onNextMonth)
        WeekdayRow()
        MonthGrid(month = month, calendar = calendar, today = today)
        if (isLoading) {
            Text(
                text = "불러오는 중…",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        StatusLegend()
    }
}

@Composable
private fun MonthHeader(
    month: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "◀",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.small,
            modifier = Modifier.singleClickable(onClick = onPrevMonth).padding(horizontal = 6.dp),
        )
        Text(
            text = monthTitle(month),
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "▶",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.small,
            modifier = Modifier.singleClickable(onClick = onNextMonth).padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun WeekdayRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        WEEKDAYS.forEach { label ->
            Text(
                text = label,
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 6주 격자를 고정으로 그리지 않고 **그 달에 필요한 줄 수만** 그린다 — 달마다 카드 높이가
 * 달라지는 편이, 빈 줄 하나가 늘 붙어 있는 것보다 낫다.
 */
@Composable
private fun MonthGrid(
    month: String,
    calendar: ChallengeCalendar?,
    today: LocalDate,
) {
    val yearMonth = parseMonth(month) ?: return
    val first = yearMonth.atDay(1)
    // 일요일 시작. DayOfWeek 는 월=1..일=7 이라 7을 0으로 접는다.
    val leading = first.dayOfWeek.value % 7
    val length = yearMonth.lengthOfMonth()
    val cells = leading + length
    val rows = (cells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val dayNumber = row * 7 + column - leading + 1
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (dayNumber in 1..length) {
                            val date = yearMonth.atDay(dayNumber)
                            DayCell(
                                dayNumber = dayNumber,
                                status = calendar?.dayOf(date.toString())?.status,
                                isToday = date == today,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    dayNumber: Int,
    status: ChallengeDayStatus?,
    isToday: Boolean,
) {
    val fill = status.fillColor
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(CircleShape)
                .background(fill ?: Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$dayNumber",
            color =
                when {
                    fill != null -> RuleUpTheme.colors.onSuccess
                    isToday -> RuleUpTheme.colors.brand
                    else -> RuleUpTheme.colors.textMuted
                },
            style = RuleUpTheme.typography.caption,
        )
    }
}

@Composable
private fun StatusLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LegendItem(label = "완료", status = ChallengeDayStatus.DONE)
        LegendItem(label = "실패 예정", status = ChallengeDayStatus.CHECKING)
        LegendItem(label = "실패", status = ChallengeDayStatus.FAILED)
    }
}

@Composable
private fun LegendItem(
    label: String,
    status: ChallengeDayStatus,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .height(8.dp)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(status.fillColor ?: RuleUpTheme.colors.surfaceVariant),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

/**
 * 칸 색. **판정 대상이 아닌 날과 오늘은 색을 갖지 않는다** — 아직 결과가 없는 것이지 실패가
 * 아니다. 모르는 상태(서버가 enum 을 늘린 경우)도 같은 이유로 비운다.
 */
private val ChallengeDayStatus?.fillColor: Color?
    @Composable
    get() =
        when (this) {
            ChallengeDayStatus.DONE -> RuleUpTheme.colors.success
            ChallengeDayStatus.FAILED -> RuleUpTheme.colors.danger
            ChallengeDayStatus.CHECKING -> RuleUpTheme.colors.warning
            ChallengeDayStatus.IN_PROGRESS, null -> null
        }

/** "2026-09" → "2026년 9월". 파싱 못 하면 원문 그대로 — 지어내지 않는다. */
internal fun monthTitle(month: String): String {
    val parsed = parseMonth(month) ?: return month
    return "${parsed.year}년 ${parsed.monthValue}월"
}

internal fun parseMonth(month: String): YearMonth? =
    try {
        YearMonth.parse(month)
    } catch (_: DateTimeParseException) {
        null
    }

private val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")
