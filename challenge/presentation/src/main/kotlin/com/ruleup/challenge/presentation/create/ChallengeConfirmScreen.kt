package com.ruleup.challenge.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.create.component.CreateChallengeTopBar
import com.ruleup.challenge.presentation.create.component.DurationPickerSheet
import com.ruleup.challenge.presentation.create.component.GradientSwitch
import com.ruleup.challenge.presentation.create.component.InfoNote
import com.ruleup.challenge.presentation.create.component.ParamsEditor
import com.ruleup.challenge.presentation.create.component.SectionLabel
import com.ruleup.challenge.presentation.create.component.SmallBadge
import com.ruleup.challenge.presentation.create.component.rememberChallengeImagePicker
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.challenge.presentation.create.viewmodel.TextEditField
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 확인 화면 — 초안을 항목별로 보여주고 고친 뒤 생성한다. 두 진입 경로(추천 칩 · 설명 입력)가 여기로 수렴한다.
 *
 * 잠긴 항목은 회색 처리로 끝내지 않고 **잠금 아이콘과 사유를 함께** 보여준다. 생성 CTA 는 하단 고정이라
 * 끝까지 스크롤해야 누를 수 있는 구조를 만들지 않는다.
 */
@Composable
fun ChallengeConfirmContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    state: CreateChallengeState = CreateChallengeState.initial,
) {
    val nav = LocalNavigationHelper.current
    var showPeriodSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(RuleUpTheme.colors.background)) {
        CreateChallengeTopBar(
            title = "챌린지 확인",
            onBack = { nav.navigateToBack() },
            trailingText = "취소",
            onTrailingClick = { nav.navigateToBack() },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { TitleSection(state = state, onIntent = onIntent) }
            item { DescriptionSection(state = state, onIntent = onIntent) }
            item { CoverPhotoSection(coverImageUri = state.coverImageUri, onIntent = onIntent) }
            item { CategorySection(state = state) }
            item { ModeSection(state = state, onIntent = onIntent) }
            if (state.isGroup) {
                item { VisibilitySection(state = state, onIntent = onIntent) }
                item { CapacitySection(capacity = state.capacity, onIntent = onIntent) }
                item { MinTierSection(state = state, onIntent = onIntent) }
            } else {
                item { RankingVisibleSection(state = state, onIntent = onIntent) }
            }
            item { PeriodSection(state = state, onClick = { showPeriodSheet = true }) }
            if (state.params.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("목표")
                        ParamsEditor(
                            params = state.params,
                            onEdit = { key, value -> onIntent(CreateChallengeIntent.EditParam(key, value)) },
                        )
                    }
                }
            }
            item { VerificationSection(state = state, onIntent = onIntent) }
            item { PenaltySection(state = state, onIntent = onIntent) }
            item {
                InfoNote(
                    emoji = "✨",
                    text = "카테고리는 만든 뒤에는 바꿀 수 없어요. 나머지는 시작 전 혼자일 때 수정할 수 있어요",
                    background = RuleUpPalette.Primary50,
                    textColor = RuleUpTheme.colors.textSlate,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            RuleUpPrimaryButton(
                text = if (state.isCreating) "만드는 중…" else "이대로 만들기",
                enabled = state.hasDraft && !state.isCreating,
                onClick = { onIntent(CreateChallengeIntent.Create) },
            )
        }
    }

    if (showPeriodSheet) {
        // 시트는 기간 길이로 고르고, 계약이 요구하는 (start, end) 로 바꿔 올린다.
        DurationPickerSheet(
            startDate = state.period.start,
            durationDays = ChallengeDates.daysBetween(state.period.start, state.period.end) + 1,
            onConfirm = { start, duration ->
                onIntent(CreateChallengeIntent.SetPeriod(start, ChallengeDates.endDate(start, duration)))
                showPeriodSheet = false
            },
            onDismiss = { showPeriodSheet = false },
        )
    }
}

@Composable
private fun TitleSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("제목") {
            // 원본 그대로면 "AI 생성", 고쳤으면 "수정함". 되돌리면 다시 AI 생성으로 돌아간다.
            if (state.titleEdited) {
                SmallBadge("수정함", RuleUpTheme.colors.warningContainer, RuleUpTheme.colors.warning)
            } else {
                SmallBadge("AI 생성", RuleUpPalette.Primary50, RuleUpTheme.colors.brand)
            }
        }
        BoxedTextField(
            value = state.title,
            onValueChange = { onIntent(CreateChallengeIntent.SetTitle(it)) },
            onFocusLeave = { onIntent(CreateChallengeIntent.ConfirmTextEdit(TextEditField.TITLE)) },
        )
    }
}

@Composable
private fun DescriptionSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("설명") {
            if (state.descriptionEdited) {
                SmallBadge("수정함", RuleUpTheme.colors.warningContainer, RuleUpTheme.colors.warning)
            }
        }
        BoxedTextField(
            value = state.description,
            onValueChange = { onIntent(CreateChallengeIntent.SetDescription(it)) },
            minHeight = 88.dp,
            onFocusLeave = { onIntent(CreateChallengeIntent.ConfirmTextEdit(TextEditField.DESCRIPTION)) },
        )
    }
}

@Composable
private fun BoxedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp,
    onFocusLeave: () -> Unit = {},
) {
    // 포커스가 빠지는 순간을 잡는다 — 타이핑마다 수정 로그를 보내지 않기 위해서다.
    var hadFocus by remember { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(minHeight)
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
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
            textStyle = RuleUpTheme.typography.body.copy(color = RuleUpTheme.colors.textPrimary),
        )
    }
}

@Composable
private fun CoverPhotoSection(
    coverImageUri: String?,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberChallengeImagePicker { onIntent(CreateChallengeIntent.SetCoverImage(it)) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("대표 이미지") {
            SmallBadge("선택", RuleUpTheme.colors.surfaceVariant, RuleUpTheme.colors.textMuted)
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surface)
                    .singleClickable { picker.launchGallery() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (coverImageUri == null) "사진 고르기" else "사진 1장 선택됨 · 다시 고르기",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}

/** 카테고리는 확인 화면부터 수정 불가이며 생성 후에도 불변이다. */
@Composable
private fun CategorySection(
    state: CreateChallengeState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("카테고리")
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                state.category?.let { Text(categoryEmoji(it), style = RuleUpTheme.typography.body) }
                Text(
                    text = state.category?.label ?: "분류 없음",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔒", style = RuleUpTheme.typography.caption)
                Text(
                    "만든 뒤 바꿀 수 없어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun ModeSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("참여 형태")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip(
                text = "혼자",
                selected = state.mode == ChallengeMode.SOLO,
                onClick = { onIntent(CreateChallengeIntent.SetMode(ChallengeMode.SOLO)) },
            )
            SelectableChip(
                text = "같이",
                selected = state.mode == ChallengeMode.GROUP,
                onClick = { onIntent(CreateChallengeIntent.SetMode(ChallengeMode.GROUP)) },
            )
        }
    }
}

@Composable
private fun VisibilitySection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("공개 범위")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip(
                text = "공개",
                selected = state.visibility != ChallengeVisibility.PRIVATE,
                onClick = { onIntent(CreateChallengeIntent.SetVisibility(ChallengeVisibility.PUBLIC)) },
            )
            SelectableChip(
                text = "비공개",
                selected = state.visibility == ChallengeVisibility.PRIVATE,
                onClick = { onIntent(CreateChallengeIntent.SetVisibility(ChallengeVisibility.PRIVATE)) },
            )
        }
        if (state.visibility == ChallengeVisibility.PRIVATE) {
            Text(
                "비공개 방은 초대 링크로만 들어올 수 있어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun RankingVisibleSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    ToggleRow(
        modifier = modifier,
        label = "랭킹에 내 기록 보이기",
        checked = state.rankingVisible ?: true,
        onCheckedChange = { onIntent(CreateChallengeIntent.SetRankingVisible(it)) },
    )
}

/** 정원 스테퍼. 10,000까지 버튼으로만 올리게 하지 않고 직접 입력도 허용한다. */
@Composable
private fun CapacitySection(
    capacity: Int,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("최대 인원")
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surface)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton("−") { onIntent(CreateChallengeIntent.SetCapacity(capacity - 1)) }
            BasicTextField(
                value = capacity.toString(),
                onValueChange = { input ->
                    input.filter(Char::isDigit).toIntOrNull()?.let {
                        onIntent(CreateChallengeIntent.SetCapacity(it))
                    }
                },
                textStyle = RuleUpTheme.typography.bodyBold.copy(color = RuleUpTheme.colors.textPrimary),
                singleLine = true,
            )
            StepperButton("+") { onIntent(CreateChallengeIntent.SetCapacity(capacity + 1)) }
        }
    }
}

/**
 * 최소 입장 티어. 상한은 **생성자 표시 티어**라 그 위는 고를 수 없다(서버도 `MIN_TIER_EXCEEDS_OWNER` 로 막는다).
 * 슬라이더는 터치만으로 다루기 어려워 라벨을 직접 탭해도 선택되게 둔다.
 */
@Composable
private fun MinTierSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cap = state.ownerTierCap
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("최소 입장 티어") {
            cap?.let {
                SmallBadge("내 티어 ${it.label()}까지", RuleUpTheme.colors.surfaceVariant, RuleUpTheme.colors.textMuted)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tier.entries.forEach { tier ->
                val selectable = cap == null || tier.ordinal <= cap.ordinal
                SelectableChip(
                    text = tier.label(),
                    selected = state.minTier == tier,
                    enabled = selectable,
                    onClick = { onIntent(CreateChallengeIntent.SetMinTier(tier)) },
                )
            }
        }
    }
}

@Composable
private fun PeriodSection(
    state: CreateChallengeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("기간")
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surface)
                    .singleClickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (state.period.start.isBlank()) {
                        "기간 고르기"
                    } else {
                        "${ChallengeDates.formatMonthDay(state.period.start)} ~ " +
                            ChallengeDates.formatMonthDay(state.period.end)
                    },
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text("›", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.body)
        }
    }
}

/** 인증 방식. 자동 → 수동은 되지만 되돌릴 수 없다는 것을 미리 알린다. */
@Composable
private fun VerificationSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("인증 방식")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectableChip(
                text = "자동 인증",
                selected = state.isAuto,
                // 초안이 자동 인증으로 왔으면 되돌릴 수 있다 — 단방향 잠금은 생성 이후 수정 화면의 규칙이다.
                enabled = state.canUseAuto,
                onClick = { onIntent(CreateChallengeIntent.SetVerificationType(VerificationType.AUTO)) },
            )
            SelectableChip(
                text = "직접 체크",
                selected = !state.isAuto,
                onClick = { onIntent(CreateChallengeIntent.SetVerificationType(VerificationType.MANUAL)) },
            )
        }
        if (state.isAuto) {
            Text(
                "직접 체크로 바꾸면 되돌릴 수 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

/**
 * 패널티. `score`·`groupShare` 는 **서버가 강제**하는 값이라 잠근 채로 보여준다(인지 목적).
 * 고를 수 있는 건 감시자 통지 하나뿐이다.
 */
@Composable
private fun PenaltySection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("실패했을 때")
        ToggleRow(
            label = "점수 차감",
            checked = state.penalties.score,
            enabled = false,
            caption = "자동 인증 방은 항상 켜져 있어요",
            onCheckedChange = {},
        )
        ToggleRow(
            label = "같이 하는 사람에게 공유",
            checked = state.penalties.groupShare,
            enabled = false,
            caption = "같이 하는 방은 항상 켜져 있어요",
            onCheckedChange = {},
        )
        ToggleRow(
            label = "감시자에게 알리기",
            checked = state.penalties.watcher,
            onCheckedChange = { onIntent(CreateChallengeIntent.SetWatcherPenalty(it)) },
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    caption: String? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!enabled) Text("🔒", style = RuleUpTheme.typography.caption)
                Text(label, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            }
            caption?.let {
                Text(it, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
            }
        }
        GradientSwitch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier =
            modifier
                .clip(RuleUpTheme.shapes.chip)
                .background(
                    when {
                        selected -> RuleUpTheme.colors.brand
                        !enabled -> RuleUpTheme.colors.surfaceVariant
                        else -> RuleUpTheme.colors.surface
                    },
                ).singleClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color =
                when {
                    selected -> RuleUpTheme.colors.onSuccess
                    !enabled -> RuleUpTheme.colors.textMuted
                    else -> RuleUpTheme.colors.textSecondary
                },
            style = RuleUpTheme.typography.smallMedium,
        )
    }
}

@Composable
private fun StepperButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .size(32.dp)
                .clip(RuleUpTheme.shapes.pill)
                .background(RuleUpTheme.colors.surfaceVariant)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyBold)
    }
}

private fun Tier.label(): String =
    when (this) {
        Tier.BRONZE -> "브론즈"
        Tier.SILVER -> "실버"
        Tier.GOLD -> "골드"
        Tier.DIAMOND -> "다이아"
        Tier.RUBY -> "루비"
    }
