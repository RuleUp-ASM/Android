package com.ruleup.profile.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.ruleup.designsystem.category.CategoryIconTile
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.time.ServiceDate
import com.ruleup.profile.domain.entity.CalendarDay
import com.ruleup.profile.domain.entity.CalendarDayDetail
import com.ruleup.profile.domain.entity.CalendarDayItem
import com.ruleup.profile.domain.entity.CalendarDayStatus
import com.ruleup.profile.domain.entity.DayItemStatus
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarIntent
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarState
import com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.verification.domain.entity.failureTextOf
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// 달력 관례로 토요일은 파랑이다.
private val SaturdayBlue = Color(0xFF3B82F6)

/** 활동 캘린더. */
@Composable
fun MyCalendarScreen(
    modifier: Modifier = Modifier,
    date: String? = null,
    viewModel: MyCalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyCalendarIntent.Load(date))
    }

    MyCalendarContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
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
            // 실패를 그리지 않으면 조회 실패가 「기록 없는 달」과 똑같아 보인다(#403).
            state.errorMessage?.let { message ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = message, color = RuleUpTheme.colors.danger, style = RuleUpTheme.typography.caption)
                    Text(
                        text = "다시 시도",
                        color = RuleUpTheme.colors.brand,
                        style = RuleUpTheme.typography.bodyBold,
                        modifier = Modifier.singleClickable { onIntent(MyCalendarIntent.Retry) },
                    )
                }
            }
            MonthGrid(
                month = month,
                days = state.days,
                selectedDate = state.selectedDate,
                isLoading = state.isLoading,
                onSelect = { onIntent(MyCalendarIntent.SelectDate(it)) },
            )
            Legend()
            // 인증 대상이 아닌 날은 알릴 것이 없어 카드를 띄우지 않는다
            val selected = state.selectedDate
            val selectedDay = state.selectedDay
            if (selected != null && selectedDay != null) {
                DayDetailCard(
                    onAppeal = { onIntent(MyCalendarIntent.OpenAppeal(it)) },
                    date = selected,
                    day = selectedDay,
                    detail = state.dayDetail,
                    isLoading = state.isLoadingDetail || (state.isLoading && selected.take(7) == state.month),
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
    // 월 이동은 상단 화살표(ChangeMonth 인텐트)로만
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
                modifier = Modifier.ttiContentDrawn(),
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
    // 판정 경계가 KST 하루 단위다.
    val isToday = date == ServiceDate.today()
    // 칸 폭을 채워 요일 라벨과 중심을 맞추고, 정사각형이라야 선택 표시가 원이 된다.
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(2.dp)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(if (isSelected) RuleUpTheme.colors.brand else Color.Transparent)
                .singleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
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
        // 실패 예정은 확정 전이라 실패가 아니다.
        CalendarDayStatus.IN_PROGRESS,
        CalendarDayStatus.FAIL_EXPECTED,
        -> if (isSelected) RuleUpPalette.BgSurface else RuleUpTheme.colors.brand
        // 판정 대상일만 내려오므로 null 은 비대상일이거나 모르는 값이다
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
    day: CalendarDay,
    detail: CalendarDayDetail?,
    isLoading: Boolean,
    onAppeal: (String) -> Unit,
) {
    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
    val title =
        buildString {
            if (parsed != null) {
                append("${parsed.monthValue}월 ${parsed.dayOfMonth}일")
                if (parsed == ServiceDate.today()) append(" (오늘)")
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
                    DayItemRow(item = item, onAppeal = onAppeal)
                }
        }
    }
}

@Composable
private fun DayItemRow(
    item: CalendarDayItem,
    onAppeal: (String) -> Unit,
) {
    val (statusLabel, statusColor) =
        when (item.status) {
            DayItemStatus.DONE ->
                buildString {
                    item.confirmedAt?.let { append("${it.timeLabel()} ") }
                    append("완료")
                } to RuleUpTheme.colors.success

            // 문구 표는 인증 모듈이 갖는다
            DayItemStatus.FAILED -> failureTextOf(item.failureReason) to RuleUpTheme.colors.danger
            // 확정 실패와 같은 색을 쓰지 않는다
            DayItemStatus.FAIL_EXPECTED -> "실패 예정" to RuleUpPalette.StatusWarn
            DayItemStatus.IN_PROGRESS -> "진행 중" to RuleUpTheme.colors.brand
            // 모르는 상태는 완료·실패 어느 쪽으로도 접지 않고 표기를 생략한다.
            null -> "" to RuleUpTheme.colors.textMuted
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CategoryIconTile(category = item.category, size = 36.dp)
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
        // 낼 수 있는 건에만 붙인다.
        if (item.appeal?.eligible == true && item.verificationId != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = "이의 제기",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.captionBold,
                modifier = Modifier.singleClickable { onAppeal(item.challengeId) },
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

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun MyCalendarContentPreview() {
    RuleUpTheme {
        MyCalendarContent(
            state =
                com.ruleup.profile.presentation.calendar.viewmodel.MyCalendarState.initial.copy(
                    isLoading = false,
                ),
            onIntent = {
            },
        )
    }
}
