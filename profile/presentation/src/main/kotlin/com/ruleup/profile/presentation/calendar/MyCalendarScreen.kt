package com.ruleup.profile.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.profile.domain.entity.DayItemStatus
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarIntent
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarState
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// 달력 관례로 토요일은 파랑이다. Figma 팔레트 15색에 파랑이 없어 화면이 들고 있는다.
private val SaturdayBlue = Color(0xFF3B82F6)

/**
 * 활동 캘린더 (피그마 434:361). kizitonwose Calendar-Compose 월 그리드 + 일자 상세.
 * day status 는 서버 판정 값 그대로 — 확정된 날짜 기준이라 클라 재계산이 없다.
 */
@Composable
fun MyCalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: MyCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyCalendarIntent.Load)
    }

    MyCalendarContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 상태를 받아 그리기만 한다 — ViewModel 을 직접 꺼내지 않아 상태별 렌더를 그대로 검증할 수 있다. */
@Composable
internal fun MyCalendarContent(
    state: MyCalendarState,
    onIntent: (MyCalendarIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "활동 캘린더", onBack = { onIntent(MyCalendarIntent.Back) })

        val month = runCatching { YearMonth.parse(state.month) }.getOrNull()
        if (month == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RuleUpTheme.colors.brand)
            }
            return@Column
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthHeader(
                month = month,
                onPrev = { onIntent(MyCalendarIntent.ChangeMonth(-1)) },
                onNext = { onIntent(MyCalendarIntent.ChangeMonth(1)) },
            )
            MonthGrid(
                month = month,
                days = state.days,
                selectedDate = state.selectedDate,
                isLoading = state.isLoading,
                onSelect = { onIntent(MyCalendarIntent.SelectDate(it)) },
            )
            Legend()
            state.selectedDate?.let { selected ->
                DayDetailCard(
                    date = selected,
                    day = state.days[selected],
                    detail = state.dayDetail,
                    isLoading = state.isLoadingDetail,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onPrev),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "‹", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.section)
        }
        Text(
            text = "${month.year}년 ${month.monthValue}월",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onNext),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "›", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.section)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    days: Map<String, CalendarDay>,
    selectedDate: String?,
    isLoading: Boolean,
    onSelect: (String) -> Unit,
) {
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.SUNDAY) }
    // 월 이동은 상단 화살표(ChangeMonth 인텐트)로만 — 그리드는 표시 중인 한 달만 렌더링한다.
    val calendarState =
        rememberCalendarState(
            startMonth = month,
            endMonth = month,
            firstVisibleMonth = month,
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

    RuleUpCard(
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREAN),
                    color =
                        when (dayOfWeek) {
                            DayOfWeek.SUNDAY -> RuleUpTheme.colors.danger
                            DayOfWeek.SATURDAY -> SaturdayBlue
                            else -> RuleUpTheme.colors.textSecondary
                        },
                    style = RuleUpTheme.typography.smallBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        if (isLoading) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = RuleUpTheme.colors.brand)
            }
        } else {
            HorizontalCalendar(
                state = calendarState,
                userScrollEnabled = false,
                dayContent = { day ->
                    if (day.position == DayPosition.MonthDate) {
                        DayCell(
                            date = day.date,
                            status = days[day.date.toString()]?.status,
                            isSelected = day.date.toString() == selectedDate,
                            onClick = { onSelect(day.date.toString()) },
                        )
                    } else {
                        Spacer(Modifier.size(40.dp))
                    }
                },
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    status: CalendarDayStatus?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isToday = date == LocalDate.now()
    Column(
        modifier =
            Modifier
                .padding(2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) RuleUpTheme.colors.brand else Color.Transparent)
                .singleClickable(onClick = onClick)
                .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "${date.dayOfMonth}",
            color =
                when {
                    isSelected -> RuleUpPalette.BgSurface
                    isToday -> RuleUpTheme.colors.brand
                    else -> RuleUpTheme.colors.textPrimary
                },
            style = if (isSelected || isToday) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
        )
        Box(
            modifier =
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(status.dotColor(isSelected)),
        )
    }
}

@Composable
private fun CalendarDayStatus?.dotColor(isSelected: Boolean): Color =
    when (this) {
        CalendarDayStatus.ALL_DONE -> RuleUpTheme.colors.success
        CalendarDayStatus.PARTIAL -> RuleUpPalette.StatusWarn
        CalendarDayStatus.FAILED -> RuleUpTheme.colors.danger
        // 검사중은 최종 재평가 구간이라 실패가 아니다. 미확정 색을 함께 쓰고 범례를 늘리지 않는다
        // (프론트엔드 테크스펙: 장시간 구간 범례를 추가하지 않고 카드에서만 안내).
        CalendarDayStatus.IN_PROGRESS,
        CalendarDayStatus.CHECKING,
        -> if (isSelected) RuleUpPalette.BgSurface else RuleUpTheme.colors.brand
        // 판정 대상일만 내려오므로 null 은 비대상일이거나 모르는 값이다 — 어느 쪽이든 칠하지 않는다.
        null -> Color.Transparent
    }

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendItem(color = RuleUpTheme.colors.success, label = "성공")
        LegendItem(color = RuleUpPalette.StatusWarn, label = "일부 성공")
        LegendItem(color = RuleUpTheme.colors.danger, label = "실패")
        LegendItem(color = RuleUpTheme.colors.brand, label = "대기")
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
    }
}

@Composable
private fun DayDetailCard(
    date: String,
    day: CalendarDay?,
    detail: CalendarDayDetail?,
    isLoading: Boolean,
) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val title =
        buildString {
            if (parsed != null) {
                append("${parsed.monthValue}월 ${parsed.dayOfMonth}일")
                if (parsed == LocalDate.now()) append(" (오늘)")
            } else {
                append(date)
            }
        }
    RuleUpCard {
        Text(
            text = title,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.smallBold,
        )
        when {
            day == null ->
                Text(
                    text = "인증 대상일이 아니에요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.small,
                )

            isLoading ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand, modifier = Modifier.size(22.dp))
                }

            detail == null || detail.items.isEmpty() ->
                Text(
                    text = "기록이 없어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.small,
                )

            else ->
                detail.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = RuleUpTheme.colors.border)
                    DayItemRow(item = item)
                }
        }
    }
}

@Composable
private fun DayItemRow(item: CalendarDayItem) {
    val (statusLabel, statusColor) =
        when (item.status) {
            DayItemStatus.DONE ->
                buildString {
                    item.confirmedAt?.let { append("${it.timeLabel()} ") }
                    append("완료")
                } to RuleUpTheme.colors.success

            DayItemStatus.FAILED -> (item.failureReason?.failureLabel() ?: "실패") to RuleUpTheme.colors.danger
            // 검사중을 실패처럼 보이게 하지 않는다 — 성공·실패 양쪽으로 열려 있는 상태다.
            DayItemStatus.CHECKING -> "결과 계산 중" to RuleUpPalette.StatusWarn
            DayItemStatus.IN_PROGRESS -> "진행 중" to RuleUpTheme.colors.brand
            // 모르는 상태는 완료·실패 어느 쪽으로도 접지 않고 표기를 생략한다.
            null -> "" to RuleUpTheme.colors.textMuted
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = item.category?.let(::categoryEmoji) ?: "🎯", style = RuleUpTheme.typography.section)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = item.title,
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = statusLabel,
                color = statusColor,
                style = RuleUpTheme.typography.captionBold,
            )
        }
    }
}

// "2026-05-27T06:13:00+09:00" → "06:13"
private fun String.timeLabel(): String {
    val time = substringAfter('T', missingDelimiterValue = "")
    if (time.length < 5) return ""
    return time.take(5)
}

// 실패 사유 코드 → 사용자 문구 (미지 코드는 일반 문구)
private fun String.failureLabel(): String =
    when (this) {
        "NO_SIGNAL_RECEIVED" -> "인증 신호가 감지되지 않았어요"
        "MISSED_WINDOW" -> "인증 시간을 놓쳤어요"
        else -> "실패"
    }
