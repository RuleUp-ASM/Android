package com.ruleup.challenge.presentation.create.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeLimits
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.create.ChallengeDates
import com.ruleup.challenge.presentation.create.label
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.challenge.presentation.create.viewmodel.TextEditField
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import kotlin.math.roundToInt

/** 확인 화면 요약 줄에 대응하는 편집 시트 (Figma 1134:669 — 4종). */
enum class ConfirmEditSection {
    TITLE_DESCRIPTION,
    MODE_CAPACITY,
    VERIFICATION,
    PERIOD,
    PENALTIES,
}

/**
 * 항목별 편집 바텀시트.
 *
 * 값은 **타이핑 즉시 상태에 반영**하고 "저장" 은 시트를 닫는 역할만 한다 — 확인 화면 전체가 아직
 * 서버에 올라가지 않은 초안이라, 여기서 따로 커밋 개념을 만들면 "저장했는데 왜 또 만들기를 눌러야
 * 하냐" 는 혼동만 생긴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmEditSheet(
    section: ConfirmEditSection,
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 기간은 캘린더가 필요해 기존 전용 시트를 그대로 쓴다.
    var showPeriodPicker by remember { mutableStateOf(false) }

    if (showPeriodPicker) {
        DurationPickerSheet(
            startDate = state.period.start,
            durationDays = ChallengeDates.daysBetween(state.period.start, state.period.end) + 1,
            onConfirm = { start, duration ->
                onIntent(CreateChallengeIntent.SetPeriod(start, ChallengeDates.endDate(start, duration)))
                showPeriodPicker = false
            },
            onDismiss = { showPeriodPicker = false },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = RuleUpTheme.colors.surface,
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = section.title(),
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.numberS,
            )
            when (section) {
                ConfirmEditSection.TITLE_DESCRIPTION -> TitleDescriptionEditor(state, onIntent)
                ConfirmEditSection.MODE_CAPACITY -> ModeCapacityEditor(state, onIntent)
                ConfirmEditSection.VERIFICATION -> VerificationEditor(state, onIntent)
                ConfirmEditSection.PERIOD ->
                    PeriodEditor(state, onIntent) { showPeriodPicker = true }

                ConfirmEditSection.PENALTIES -> PenaltyEditor(state, onIntent)
            }
            SheetSaveButton(onClick = onDismiss)
        }
    }
}

private fun ConfirmEditSection.title(): String =
    when (this) {
        ConfirmEditSection.TITLE_DESCRIPTION -> "이름과 설명"
        ConfirmEditSection.MODE_CAPACITY -> "모드와 인원"
        ConfirmEditSection.VERIFICATION -> "인증 방법"
        ConfirmEditSection.PERIOD -> "빈도와 기간"
        ConfirmEditSection.PENALTIES -> "실패하면"
    }

// ---------- 이름과 설명 ----------

@Composable
private fun TitleDescriptionEditor(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedField(
            value = state.title,
            onValueChange = { onIntent(CreateChallengeIntent.SetTitle(it)) },
            onFocusLeave = { onIntent(CreateChallengeIntent.ConfirmTextEdit(TextEditField.TITLE)) },
            // 제목은 이 시트에서 가장 먼저 고치는 값이라 브랜드 테두리로 강조한다.
            emphasized = true,
            placeholder = "챌린지 이름",
        )
        OutlinedField(
            value = state.description,
            onValueChange = { onIntent(CreateChallengeIntent.SetDescription(it)) },
            onFocusLeave = { onIntent(CreateChallengeIntent.ConfirmTextEdit(TextEditField.DESCRIPTION)) },
            minHeight = 72.dp,
            placeholder = "어떤 루틴인지 설명해주세요",
        )
    }
}

// ---------- 모드와 인원 ----------

@Composable
private fun ModeCapacityEditor(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    var showTiers by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedControl(
            options = listOf("그룹", "솔로"),
            selectedIndex = if (state.isGroup) 0 else 1,
            onSelect = { index ->
                onIntent(
                    CreateChallengeIntent.SetMode(
                        if (index == 0) ChallengeMode.GROUP else ChallengeMode.SOLO,
                    ),
                )
            },
        )
        // 정원·티어는 그룹 전용 계약이다. 솔로에서 보여주면 보내지지도 않을 값을 고르게 하는 셈이다.
        if (state.isGroup) {
            BorderedRow {
                Text("정원", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepperBox(
                        text = "−",
                        highlighted = false,
                        onClick = { onIntent(CreateChallengeIntent.SetCapacity(state.capacity - 1)) },
                        enabled = state.capacity > ChallengeLimits.CAPACITY_MIN,
                    )
                    Text(
                        "${state.capacity}명",
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.numberS,
                    )
                    StepperBox(
                        text = "＋",
                        highlighted = true,
                        onClick = { onIntent(CreateChallengeIntent.SetCapacity(state.capacity + 1)) },
                        enabled = state.capacity < ChallengeLimits.CAPACITY_MAX,
                    )
                }
            }
            BorderedRow(onClick = { showTiers = !showTiers }) {
                Text("참여 가능 티어", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
                Text(
                    text = (state.minTier?.label() ?: "제한 없음") + " ▾",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.bodyMedium,
                )
            }
            if (showTiers) {
                // 상한은 생성자 표시 티어다 — 그 위를 고르면 서버가 MIN_TIER_EXCEEDS_OWNER 로 막는다.
                val cap = state.ownerTierCap
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tier.entries.forEach { tier ->
                        val selectable = cap == null || tier.ordinal <= cap.ordinal
                        ChoiceChip(
                            text = tier.label(),
                            selected = state.minTier == tier,
                            enabled = selectable,
                            onClick = { onIntent(CreateChallengeIntent.SetMinTier(tier)) },
                        )
                    }
                }
            }
        }
    }
}

// ---------- 인증 방법 ----------

@Composable
private fun VerificationEditor(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedControl(
            options = listOf("자동 인증", "수동 체크"),
            selectedIndex = if (state.isAuto) 0 else 1,
            // 초안이 수동으로 왔으면 자동을 켤 수 없다 — 그 루틴은 자동 인증을 지원하지 않는다.
            enabledIndices = if (state.canUseAuto) setOf(0, 1) else setOf(1),
            onSelect = { index ->
                onIntent(
                    CreateChallengeIntent.SetVerificationType(
                        if (index == 0) VerificationType.AUTO else VerificationType.MANUAL,
                    ),
                )
            },
        )
        // 목표값은 루틴 템플릿이 정한다 — 키별 화면을 하드코딩하지 않고 kind·unit·min·max 로 그린다.
        if (state.params.isNotEmpty()) {
            ParamsEditor(
                params = state.params,
                onEdit = { key, value -> onIntent(CreateChallengeIntent.EditParam(key, value)) },
            )
        }
        Text(
            text =
                if (state.isAuto) {
                    "장소·대상 앱은 참여자가 각자 등록해요 (첫 입장 시)"
                } else {
                    "수동 체크는 점수에 반영되지 않고 기록만 남아요"
                },
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

// ---------- 빈도와 기간 ----------

@Composable
private fun PeriodEditor(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    onPickPeriod: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 빈도는 **요일이 아니라 그 주에 몇 번**이다. 판정 주기가 1주 고정이라 어느 날 하든 상관없고,
        // 요일 체크박스를 두면 지키지도 않을 요일을 고르게 만든다.
        WeeklyCountSlider(
            count = state.weeklyCount,
            onChange = { onIntent(CreateChallengeIntent.SetWeeklyCount(it)) },
        )
        BorderedRow(onClick = onPickPeriod) {
            Text("시작일 · 기간", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            val label =
                if (state.period.start.isBlank()) {
                    "고르기 ▾"
                } else {
                    val days = ChallengeDates.daysBetween(state.period.start, state.period.end) + 1
                    "${ChallengeDates.formatMonthDay(state.period.start)} · ${ChallengeDates.durationLabel(days)} ▾"
                }
            Text(label, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
        }
    }
}

private val weeklyCountRange =
    ChallengeLimits.WEEKLY_COUNT_MIN.toFloat()..ChallengeLimits.WEEKLY_COUNT_MAX.toFloat()

/**
 * 눈금이 7칸뿐이라 [Slider] 의 `steps` 로 딱 떨어지게 잡고, 숫자 라벨을 직접 탭해도 선택되게 둔다 —
 * 슬라이더 손잡이만으로 한 칸을 정확히 맞추는 건 손가락으로 하기 번거롭다.
 */
@Composable
private fun WeeklyCountSlider(
    count: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("주간 횟수", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            Text(
                text = if (count >= ChallengeLimits.WEEKLY_COUNT_MAX) "매일" else "주 ${count}회",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.numberS,
            )
        }
        Slider(
            value = count.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = weeklyCountRange,
            // 양 끝을 뺀 내부 눈금 수 — 1~7 이면 5개다.
            steps = ChallengeLimits.WEEKLY_COUNT_MAX - ChallengeLimits.WEEKLY_COUNT_MIN - 1,
            colors =
                SliderDefaults.colors(
                    thumbColor = RuleUpTheme.colors.brand,
                    activeTrackColor = RuleUpTheme.colors.brand,
                    inactiveTrackColor = RuleUpTheme.colors.border,
                    activeTickColor = RuleUpTheme.colors.brandSoft,
                    inactiveTickColor = RuleUpTheme.colors.borderStrong,
                ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            (ChallengeLimits.WEEKLY_COUNT_MIN..ChallengeLimits.WEEKLY_COUNT_MAX).forEach { tick ->
                Text(
                    text = tick.toString(),
                    modifier = Modifier.singleClickable { onChange(tick) },
                    color = if (tick == count) RuleUpTheme.colors.brand else RuleUpTheme.colors.textMuted,
                    style = if (tick == count) RuleUpTheme.typography.smallBold else RuleUpTheme.typography.smallMedium,
                )
            }
        }
        Text(
            text = "요일은 고르지 않아요. 한 주 안에서 아무 날이나 채우면 돼요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

// ---------- 실패하면 ----------

/**
 * 패널티.
 *
 * `score`·`groupShare` 는 **서버가 강제**한다 — 자동 인증 방이면 점수 차감이, 그룹 방이면 그룹 공개가
 * 항상 켜지고 클라가 뭘 보내든 무시된다. 그래도 잠근 채 노출한다(무엇이 걸려 있는지 알고 만들어야 한다).
 * 고를 수 있는 건 감시자 알림 하나뿐이다.
 */
@Composable
private fun PenaltyEditor(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PenaltyToggleRow(
            label = "점수 차감",
            checked = state.penalties.score,
            enabled = false,
            caption = "자동 인증 방은 항상 켜져 있어요",
        )
        PenaltyToggleRow(
            label = "같이 하는 사람에게 공개",
            checked = state.penalties.groupShare,
            enabled = false,
            caption = "같이 하는 방은 항상 켜져 있어요",
        )
        PenaltyToggleRow(
            label = "감시자에게 알리기",
            checked = state.penalties.watcher,
            caption = "각자 등록한 감시자에게만 가요",
            onCheckedChange = { onIntent(CreateChallengeIntent.SetWatcherPenalty(it)) },
        )
    }
}

@Composable
private fun PenaltyToggleRow(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    caption: String? = null,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(if (enabled) Color.Transparent else RuleUpTheme.colors.background)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                color = if (enabled) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.bodyMedium,
            )
            caption?.let {
                Text(it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.micro)
            }
        }
        GradientSwitch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

// ---------- 공용 조각 ----------

@Composable
private fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RuleUpTheme.colors.border),
        )
    }
}

@Composable
private fun SheetSaveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RuleUpTheme.shapes.medium)
                .background(RuleUpTheme.colors.brand)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("저장", color = RuleUpTheme.colors.onSuccess, style = RuleUpTheme.typography.cardTitle)
    }
}

@Composable
private fun OutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 44.dp,
    emphasized: Boolean = false,
    placeholder: String = "",
    onFocusLeave: () -> Unit = {},
) {
    var hadFocus by remember { mutableStateOf(false) }
    val borderColor = if (emphasized) RuleUpTheme.colors.brand else RuleUpTheme.colors.border
    val borderWidth = if (emphasized) 1.5.dp else 1.dp
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RuleUpTheme.shapes.medium)
                .border(borderWidth, borderColor, RuleUpTheme.shapes.medium)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.bodyMedium)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.fillMaxWidth().onFocusChanged { focus ->
                    if (focus.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        hadFocus = false
                        onFocusLeave()
                    }
                },
            textStyle = RuleUpTheme.typography.bodyMedium.copy(color = RuleUpTheme.colors.textPrimary),
            cursorBrush = SolidColor(RuleUpTheme.colors.brand),
        )
    }
}

/** 2분할 세그먼트 컨트롤. 고를 수 없는 칸은 눌러도 반응하지 않고 흐리게 보인다. */
@Composable
private fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabledIndices: Set<Int> = options.indices.toSet(),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(RuleUpTheme.colors.border)
                .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val enabled = index in enabledIndices
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) RuleUpTheme.colors.surface else Color.Transparent,
                        ).singleClickable(enabled = enabled) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color =
                        when {
                            selected -> RuleUpTheme.colors.textPrimary
                            !enabled -> RuleUpTheme.colors.border
                            else -> RuleUpTheme.colors.textMuted
                        },
                    style = if (selected) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** 테두리 한 줄 (좌: 라벨 / 우: 값). 잠긴 줄은 배경을 깔아 눌러도 되는 줄과 구분한다. */
@Composable
private fun BorderedRow(
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(
                    if (locked) RuleUpTheme.colors.background else Color.Transparent,
                ).border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.medium)
                .let { base -> if (onClick != null) base.singleClickable(onClick = onClick) else base }
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/**
 * 증감 버튼. [enabled] 가 false 면 눌리지 않고 **색도 함께 죽인다** — 눌리는데 값이 안 바뀌는 것처럼
 * 보이면 고장으로 읽힌다.
 */
@Composable
private fun StepperBox(
    text: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val accented = highlighted && enabled
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(if (accented) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.background)
                .singleClickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color =
                when {
                    !enabled -> RuleUpTheme.colors.textMuted.copy(alpha = DISABLED_ALPHA)
                    highlighted -> RuleUpTheme.colors.brand
                    else -> RuleUpTheme.colors.textMuted
                },
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

private const val DISABLED_ALPHA = 0.4f

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .height(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surface)
                .border(
                    if (selected) 1.5.dp else 1.dp,
                    if (selected) RuleUpTheme.colors.brand else RuleUpTheme.colors.border,
                    RoundedCornerShape(10.dp),
                ).singleClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color =
                when {
                    selected -> RuleUpTheme.colors.brand
                    !enabled -> RuleUpTheme.colors.border
                    else -> RuleUpTheme.colors.textMuted
                },
            style = RuleUpTheme.typography.smallMedium,
        )
    }
}
