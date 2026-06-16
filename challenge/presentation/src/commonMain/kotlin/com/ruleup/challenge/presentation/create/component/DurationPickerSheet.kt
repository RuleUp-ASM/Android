package com.ruleup.challenge.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.presentation.create.CalendarCell
import com.ruleup.challenge.presentation.create.ChallengeDates
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

private val durationPresets = listOf(7, 14, 28, 90)

/** 03 · 기간 선택 모달. 프리셋(1주/2주/4주/3개월) + 캘린더에서 시작일을 고른다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DurationPickerSheet(
    startDate: String,
    durationDays: Int,
    onConfirm: (startDate: String, durationDays: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedStart by remember { mutableStateOf(startDate.ifBlank { ChallengeDates.todayIso() }) }
    var selectedDuration by remember { mutableIntStateOf(durationDays) }
    val initialCalendar = remember { ChallengeDates.parse(selectedStart) ?: ChallengeDates.today() }
    var displayedYear by remember { mutableIntStateOf(initialCalendar.year) }
    var displayedMonth by remember { mutableIntStateOf((initialCalendar.monthNumber - 1)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = RuleUpTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier.fillMaxWidth().height(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(RuleUpTheme.colors.borderStrong),
                )
            }
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            SheetHeader(onDismiss = onDismiss)
            PresetChips(
                selectedDuration = selectedDuration,
                onSelect = { selectedDuration = it },
            )
            MonthNavigator(
                year = displayedYear,
                month = displayedMonth,
                onPrev = {
                    if (displayedMonth == 0) {
                        displayedYear -= 1
                        displayedMonth = 11
                    } else {
                        displayedMonth -= 1
                    }
                },
                onNext = {
                    if (displayedMonth == 11) {
                        displayedYear += 1
                        displayedMonth = 0
                    } else {
                        displayedMonth += 1
                    }
                },
            )
            WeekdayHeader()
            CalendarGrid(
                year = displayedYear,
                month = displayedMonth,
                startIso = selectedStart,
                endIso = ChallengeDates.endDate(selectedStart, selectedDuration),
                // 탭: 시작일만 변경(기간 유지) / 드래그: 시작~종료 범위 직접 선택(기간 자동 계산)
                onStartSelect = { selectedStart = it },
                onRangeSelect = { start, end ->
                    selectedStart = start
                    selectedDuration = ChallengeDates.daysBetween(start, end) + 1
                },
            )
            PeriodSummary(startIso = selectedStart, durationDays = selectedDuration)
            SheetButtons(
                onCancel = onDismiss,
                onConfirm = { onConfirm(selectedStart, selectedDuration) },
            )
        }
    }
}

@Composable
private fun SheetHeader(onDismiss: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📅", fontSize = 16.sp)
            Text(
                "기간 선택",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "✕",
            modifier = Modifier.clickable(onClick = onDismiss),
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PresetChips(
    selectedDuration: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        durationPresets.forEach { preset ->
            val selected = preset == selectedDuration
            Box(
                modifier =
                    Modifier
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .let { base ->
                            if (selected) {
                                base.background(RuleUpGradients.Button)
                            } else {
                                base
                                    .background(RuleUpTheme.colors.surface)
                                    .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                            }
                        }.clickable { onSelect(preset) }
                        .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    ChallengeDates.durationLabel(preset),
                    color = if (selected) Color.White else RuleUpTheme.colors.textSlate,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun MonthNavigator(
    year: Int,
    month: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthNavButton(label = "‹", onClick = onPrev)
        Text(
            "${year}년 ${month + 1}월",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        MonthNavButton(label = "›", onClick = onNext)
    }
}

@Composable
private fun MonthNavButton(
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surfaceVariant)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = RuleUpTheme.colors.textSlate, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, label ->
            Box(
                modifier = Modifier.weight(1f).height(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color =
                        when (index) {
                            0 -> RuleUpTheme.colors.danger
                            6 -> RuleUpTheme.colors.brand
                            else -> RuleUpTheme.colors.textSecondary
                        },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * 6주 캘린더 그리드. 탭하면 [onStartSelect](시작일만 변경), 한 날짜에서 다른 날짜로 드래그하면
 * [onRangeSelect](시작~종료 범위 선택)을 호출한다. 드래그 히트테스트를 위해 각 셀의 window 좌표
 * Rect 를 [onGloballyPositioned] 로 기록하고, 제스처 지점이 포함된 셀의 날짜를 찾는다.
 */
@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    startIso: String,
    endIso: String,
    onStartSelect: (String) -> Unit,
    onRangeSelect: (startIso: String, endIso: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cells = ChallengeDates.monthCells(year, month)
    // 셀 index → window 좌표 Rect. 제스처 콜백에서만 읽으므로 snapshot 상태가 아닌 일반 맵으로 둔다.
    val cellBounds = remember(cells) { mutableMapOf<Int, Rect>() }
    var gridOrigin by remember { mutableStateOf(Offset.Zero) }
    var dragAnchorIso by remember { mutableStateOf<String?>(null) }

    fun isoAt(windowOffset: Offset): String? =
        cellBounds.entries
            .firstOrNull { it.value.contains(windowOffset) }
            ?.let { cells.getOrNull(it.key)?.iso }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .onGloballyPositioned { gridOrigin = it.positionInWindow() }
                .pointerInput(cells) {
                    detectTapGestures { offset -> isoAt(gridOrigin + offset)?.let(onStartSelect) }
                }.pointerInput(cells) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val iso = isoAt(gridOrigin + offset)
                            dragAnchorIso = iso
                            if (iso != null) onRangeSelect(iso, iso)
                        },
                        onDrag = { change, _ ->
                            val anchor = dragAnchorIso
                            val iso = isoAt(gridOrigin + change.position)
                            if (anchor != null && iso != null) {
                                // ISO(yyyy-MM-dd) 는 사전순 = 날짜순이라 그대로 비교해 시작/종료를 가린다.
                                if (iso <= anchor) onRangeSelect(iso, anchor) else onRangeSelect(anchor, iso)
                            }
                        },
                        onDragEnd = { dragAnchorIso = null },
                        onDragCancel = { dragAnchorIso = null },
                    )
                },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        cells.chunked(7).forEachIndexed { weekIndex, week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEachIndexed { dayIndex, cell ->
                    val index = weekIndex * 7 + dayIndex
                    DayCell(
                        cell = cell,
                        startIso = startIso,
                        endIso = endIso,
                        modifier =
                            Modifier
                                .weight(1f)
                                .onGloballyPositioned { cellBounds[index] = it.boundsInWindow() },
                    )
                }
            }
        }
    }
}

/** 시작/종료일은 그라데이션 원, 사이 구간은 옅은 indigo 띠로 칠한다. ISO 문자열은 사전순 비교가 날짜순과 같다. */
@Composable
private fun DayCell(
    cell: CalendarCell,
    startIso: String,
    endIso: String,
    modifier: Modifier = Modifier,
) {
    val inRange = cell.iso >= startIso && cell.iso <= endIso
    val isEdge = cell.iso == startIso || cell.iso == endIso
    Box(
        modifier = modifier.height(42.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (inRange) {
            val shape =
                when {
                    startIso == endIso -> RoundedCornerShape(18.dp)
                    cell.iso == startIso -> RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                    cell.iso == endIso -> RoundedCornerShape(topEnd = 18.dp, bottomEnd = 18.dp)
                    else -> RoundedCornerShape(0.dp)
                }
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(shape)
                        .background(RuleUpTheme.colors.brandSoft),
            )
        }
        DayNumber(cell = cell, isEdge = isEdge)
    }
}

/** 캘린더 한 칸의 날짜 숫자. 시작/종료일(edge)은 그라데이션 원 안에 흰 글씨로, 그 외는 요일별 색으로 칠한다. */
@Composable
private fun DayNumber(
    cell: CalendarCell,
    isEdge: Boolean,
) {
    if (isEdge) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(RuleUpGradients.Button),
            contentAlignment = Alignment.Center,
        ) {
            Text("${cell.day}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Text(
            "${cell.day}",
            color =
                when {
                    !cell.inMonth -> RuleUpTheme.colors.textMuted
                    cell.dayOfWeek == 1 -> RuleUpTheme.colors.danger
                    cell.dayOfWeek == 7 -> RuleUpTheme.colors.brand
                    else -> RuleUpTheme.colors.textPrimary
                },
            fontSize = 13.sp,
            fontWeight = if (cell.inMonth) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

@Composable
private fun PeriodSummary(
    startIso: String,
    durationDays: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodChip(label = "시작", date = ChallengeDates.formatMonthDay(startIso))
            Text("→", color = RuleUpTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            PeriodChip(
                label = "종료",
                date = ChallengeDates.formatMonthDay(ChallengeDates.endDate(startIso, durationDays)),
            )
        }
        Box(
            modifier =
                Modifier
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RuleUpGradients.Button)
                    .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("${durationDays}일", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    date: String,
) {
    Row(
        modifier =
            Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.brandSoft)
                .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = RuleUpTheme.colors.brand,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.54.sp,
        )
        Text(
            date,
            color = RuleUpTheme.colors.brandStrong,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SheetButtons(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center,
        ) {
            Text("취소", color = RuleUpTheme.colors.textSlate, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            modifier =
                Modifier
                    .weight(2f)
                    .height(52.dp)
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpGradients.Button)
                    .clickable(onClick = onConfirm),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("기간 확정", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
