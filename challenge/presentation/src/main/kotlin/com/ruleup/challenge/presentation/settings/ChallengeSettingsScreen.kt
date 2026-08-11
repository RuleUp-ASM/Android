package com.ruleup.challenge.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeField
import com.ruleup.challenge.domain.entity.ChallengeMode
import com.ruleup.challenge.domain.entity.ChallengeVisibility
import com.ruleup.challenge.domain.entity.ModerationState
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.presentation.create.component.CreateChallengeTopBar
import com.ruleup.challenge.presentation.create.component.GradientSwitch
import com.ruleup.challenge.presentation.create.component.InfoNote
import com.ruleup.challenge.presentation.create.component.ParamsEditor
import com.ruleup.challenge.presentation.create.component.SectionLabel
import com.ruleup.challenge.presentation.create.component.SmallBadge
import com.ruleup.challenge.presentation.create.component.rememberChallengeImagePicker
import com.ruleup.challenge.presentation.settings.viewmodel.ChallengeSettingsEffect
import com.ruleup.challenge.presentation.settings.viewmodel.ChallengeSettingsIntent
import com.ruleup.challenge.presentation.settings.viewmodel.ChallengeSettingsState
import com.ruleup.challenge.presentation.settings.viewmodel.ChallengeSettingsViewModel
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.ui.helper.LocalMessageHelper
import kotlin.math.roundToInt

/**
 * 챌린지 수정 화면(방장 전용).
 *
 * **잠금은 서버가 준 `editableFields` 를 그대로 따른다** — 클라이언트가 규칙을 재구현하면 서버와
 * 어긋나는 순간 409 를 받고서야 알게 된다. 잠긴 항목은 회색 처리로 끝내지 않고 자물쇠와 사유를
 * 함께 보여준다.
 */
@Composable
fun ChallengeSettingsScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ChallengeSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(challengeId) { viewModel.onIntent(ChallengeSettingsIntent.Load(challengeId)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChallengeSettingsEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    ChallengeSettingsContent(
        modifier = modifier,
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun ChallengeSettingsContent(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        CreateChallengeTopBar(
            title = "챌린지 수정",
            onBack = { onIntent(ChallengeSettingsIntent.Back) },
        )

        when {
            state.isLoading -> CenterBox { CircularProgressIndicator(color = RuleUpTheme.colors.brand) }

            state.loaded == null ->
                CenterBox {
                    Text(
                        text = state.errorMessage ?: "설정을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.body,
                    )
                }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    state.moderationLockedSeconds?.let { seconds ->
                        item { ModerationLockNote(seconds = seconds) }
                    }
                    item { TitleSection(state = state, onIntent = onIntent) }
                    item { DescriptionSection(state = state, onIntent = onIntent) }
                    item { CoverSection(state = state, onIntent = onIntent) }
                    item { CategorySection(state = state) }
                    item { CapacitySection(state = state, onIntent = onIntent) }
                    if (state.loaded.config.mode == ChallengeMode.GROUP) {
                        item { VisibilitySection(state = state, onIntent = onIntent) }
                        item { MinTierSection(state = state, onIntent = onIntent) }
                    } else {
                        item { RankingVisibleSection(state = state, onIntent = onIntent) }
                    }
                    item { WeeklyCountSection(state = state, onIntent = onIntent) }
                    if (state.params.isNotEmpty()) {
                        item { ParamsSection(state = state, onIntent = onIntent) }
                    }
                    item { VerificationSection(state = state, onIntent = onIntent) }
                    item { WatcherPenaltySection(state = state, onIntent = onIntent) }
                    item {
                        InfoNote(
                            emoji = "🔒",
                            text = "잠긴 항목은 시작 전이고 방에 나 혼자일 때만 바꿀 수 있어요",
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
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    RuleUpPrimaryButton(
                        text = if (state.isSaving) "저장하는 중…" else "저장",
                        // 아무것도 안 바꿨으면 열지 않는다 — 빈 PATCH 는 제목·설명을 괜히 재심사에 걸리게 한다.
                        enabled = state.hasChanges && !state.isSaving && state.moderationLockedSeconds == null,
                        onClick = { onIntent(ChallengeSettingsIntent.Save) },
                    )
                }
            }
        }
    }
}

/** 반복 거부로 1시간 수정 잠금이 걸린 상태. 언제 풀리는지 명시한다. */
@Composable
private fun ModerationLockNote(seconds: Int) {
    val minutes = (seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE
    InfoNote(
        emoji = "⏳",
        text = "심사 거부가 반복돼 잠시 수정할 수 없어요. 약 ${minutes}분 뒤에 다시 시도해 주세요",
        background = RuleUpTheme.colors.warningContainer,
        textColor = RuleUpTheme.colors.warning,
    )
}

@Composable
private fun TitleSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.TITLE)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("제목") { ModerationBadge(state.moderation?.title) }
        LockableTextField(
            value = state.title,
            enabled = editable,
            onValueChange = { onIntent(ChallengeSettingsIntent.SetTitle(it)) },
        )
        if (editable) {
            Text(
                text = "제목을 고치면 다시 심사를 거쳐요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun DescriptionSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("설명") { ModerationBadge(state.moderation?.description) }
        LockableTextField(
            value = state.description,
            enabled = state.editable(ChallengeField.DESCRIPTION),
            minHeight = 88.dp,
            onValueChange = { onIntent(ChallengeSettingsIntent.SetDescription(it)) },
        )
    }
}

@Composable
private fun CoverSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.IMAGE_URL)
    val picker = rememberChallengeImagePicker { onIntent(ChallengeSettingsIntent.SetCoverImage(it)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("대표 이미지") { ModerationBadge(state.moderation?.image) }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surface)
                    .singleClickable(enabled = editable) { picker.launchGallery() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text =
                    when {
                        state.coverImageUri != null -> "새 사진 선택됨 · 다시 고르기"
                        state.removeImage -> "기본 이미지로 되돌립니다"
                        state.imageUrl != null -> "등록된 사진 있음 · 바꾸기"
                        else -> "사진 고르기"
                    },
                color = if (editable) RuleUpTheme.colors.textSecondary else RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
        if (editable && (state.imageUrl != null || state.coverImageUri != null)) {
            Text(
                text = "기본 이미지로 되돌리기",
                modifier = Modifier.singleClickable { onIntent(ChallengeSettingsIntent.RemoveCoverImage) },
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

/** 카테고리는 어떤 상황에도 수정 불가다. */
@Composable
private fun CategorySection(state: ChallengeSettingsState) {
    val category = state.loaded?.config?.category
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("카테고리")
        LockedRow(reason = "만든 뒤에는 바꿀 수 없어요") {
            category?.let { Text(categoryEmoji(it), style = RuleUpTheme.typography.body) }
            Text(
                text = category?.label ?: "분류 없음",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun CapacitySection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.CAPACITY)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("최대 인원")
        if (!editable) {
            LockedRow(reason = "지금은 바꿀 수 없어요") {
                Text("${state.capacity}명", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            }
        } else {
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
                StepButton("−") { onIntent(ChallengeSettingsIntent.SetCapacity(state.capacity - 1)) }
                BasicTextField(
                    value = state.capacity.toString(),
                    onValueChange = { input ->
                        input.filter(Char::isDigit).toIntOrNull()?.let {
                            onIntent(ChallengeSettingsIntent.SetCapacity(it))
                        }
                    },
                    textStyle = RuleUpTheme.typography.bodyBold.copy(color = RuleUpTheme.colors.textPrimary),
                    singleLine = true,
                )
                StepButton("+") { onIntent(ChallengeSettingsIntent.SetCapacity(state.capacity + 1)) }
            }
            // 이미 들어온 사람을 내보낼 수는 없으므로 하한이 현재 인원이다.
            state.participantCount?.let {
                Text(
                    text = "지금 ${it}명이 참여 중이라 그보다 줄일 수 없어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
    }
}

@Composable
private fun VisibilitySection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.VISIBILITY)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("공개 범위")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                text = "공개",
                selected = state.visibility != ChallengeVisibility.PRIVATE,
                enabled = editable,
                onClick = { onIntent(ChallengeSettingsIntent.SetVisibility(ChallengeVisibility.PUBLIC)) },
            )
            ChoiceChip(
                text = "비공개",
                selected = state.visibility == ChallengeVisibility.PRIVATE,
                enabled = editable,
                onClick = { onIntent(ChallengeSettingsIntent.SetVisibility(ChallengeVisibility.PRIVATE)) },
            )
        }
        if (!editable) LockCaption()
    }
}

@Composable
private fun RankingVisibleSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    ToggleRow(
        label = "랭킹에 내 기록 보이기",
        checked = state.rankingVisible ?: true,
        enabled = state.editable(ChallengeField.RANKING_VISIBLE),
        onCheckedChange = { onIntent(ChallengeSettingsIntent.SetRankingVisible(it)) },
    )
}

@Composable
private fun MinTierSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.MIN_TIER)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("최소 입장 티어")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Tier.entries.forEach { tier ->
                ChoiceChip(
                    text = tier.label(),
                    selected = state.minTier == tier,
                    enabled = editable,
                    onClick = { onIntent(ChallengeSettingsIntent.SetMinTier(tier)) },
                )
            }
        }
        if (!editable) LockCaption()
    }
}

/**
 * 주간 수행 횟수 (1~7). **요일이 아니라 그 주에 몇 번**이다 — 판정 주기가 1주 고정이라 어느 날 채워도 된다.
 * 시작 전 + 방장 혼자일 때만 열린다(서버가 editableFields 로 알려준다).
 */
@Composable
private fun WeeklyCountSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.WEEKLY_COUNT)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("주간 횟수") {
            Text(
                text =
                    if (state.weeklyCount >= ChallengeSettingsState.WEEKLY_COUNT_MAX) {
                        "매일"
                    } else {
                        "주 ${state.weeklyCount}회"
                    },
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.smallBold,
            )
        }
        Slider(
            value = state.weeklyCount.toFloat(),
            onValueChange = { onIntent(ChallengeSettingsIntent.SetWeeklyCount(it.roundToInt())) },
            enabled = editable,
            valueRange = weeklyCountRange,
            // 양 끝을 뺀 내부 눈금 수 — 1~7 이면 5개다.
            steps = ChallengeSettingsState.WEEKLY_COUNT_MAX - ChallengeSettingsState.WEEKLY_COUNT_MIN - 1,
            colors =
                SliderDefaults.colors(
                    thumbColor = RuleUpTheme.colors.brand,
                    activeTrackColor = RuleUpTheme.colors.brand,
                    inactiveTrackColor = RuleUpTheme.colors.border,
                ),
        )
        if (editable) {
            Text(
                text = "요일은 고르지 않아요. 한 주 안에서 아무 날이나 채우면 돼요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        } else {
            LockCaption()
        }
    }
}

/** 주간 횟수 슬라이더의 값 범위. 명세 1~7. */
private val weeklyCountRange =
    ChallengeSettingsState.WEEKLY_COUNT_MIN.toFloat()..ChallengeSettingsState.WEEKLY_COUNT_MAX.toFloat()

@Composable
private fun ParamsSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.PARAMS)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("목표")
        if (editable) {
            ParamsEditor(
                params = state.params,
                onEdit = { key, value -> onIntent(ChallengeSettingsIntent.EditParam(key, value)) },
            )
        } else {
            state.params.forEach { spec ->
                LockedRow(reason = "시작 후에는 목표를 바꿀 수 없어요") {
                    Text(
                        text = "${spec.key.replace('_', ' ')} · ${spec.value}${spec.unit.orEmpty()}",
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerificationSection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    val editable = state.editable(ChallengeField.VERIFICATION)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel("인증 방식")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                text = "자동 인증",
                selected = state.verificationType == VerificationType.AUTO,
                // 수동으로 바꾼 뒤에는 되돌릴 수 없다 — 이미 수동이면 자동 칩을 열지 않는다.
                enabled = editable && state.verificationType == VerificationType.AUTO,
                onClick = { onIntent(ChallengeSettingsIntent.SetVerificationType(VerificationType.AUTO)) },
            )
            ChoiceChip(
                text = "직접 체크",
                selected = state.verificationType == VerificationType.MANUAL,
                enabled = editable,
                onClick = { onIntent(ChallengeSettingsIntent.SetVerificationType(VerificationType.MANUAL)) },
            )
        }
        when {
            !editable -> LockCaption()
            state.verificationType == VerificationType.AUTO ->
                Text(
                    text = "직접 체크로 바꾸면 되돌릴 수 없어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
        }
    }
}

@Composable
private fun WatcherPenaltySection(
    state: ChallengeSettingsState,
    onIntent: (ChallengeSettingsIntent) -> Unit,
) {
    ToggleRow(
        label = "실패하면 감시자에게 알리기",
        checked = state.watcherPenalty,
        enabled = state.editable(ChallengeField.PENALTIES),
        onCheckedChange = { onIntent(ChallengeSettingsIntent.SetWatcherPenalty(it)) },
    )
}

/** 방장 본인 화면에서만 붙는 심사 뱃지. 심사 중에도 모집·입장·인증에는 제한이 없다. */
@Composable
private fun ModerationBadge(state: ModerationState?) {
    when (state) {
        ModerationState.IN_REVIEW ->
            SmallBadge("심사중", RuleUpTheme.colors.warningContainer, RuleUpTheme.colors.warning)

        ModerationState.REJECTED ->
            SmallBadge("수정 필요", RuleUpTheme.colors.dangerContainer, RuleUpTheme.colors.danger)

        else -> Unit
    }
}

@Composable
private fun LockableTextField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    minHeight: androidx.compose.ui.unit.Dp = 48.dp,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(minHeight)
                .clip(RuleUpTheme.shapes.small)
                .background(if (enabled) RuleUpTheme.colors.surface else RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            textStyle =
                RuleUpTheme.typography.body.copy(
                    color = if (enabled) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
                ),
        )
    }
}

/** 잠긴 값 표시. 회색 처리만 하지 않고 자물쇠와 사유를 함께 보여준다. */
@Composable
private fun LockedRow(
    reason: String,
    content: @Composable () -> Unit,
) {
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
            content()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔒", style = RuleUpTheme.typography.caption)
            Text(reason, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
        }
    }
}

@Composable
private fun LockCaption() {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("🔒", style = RuleUpTheme.typography.caption)
        Text(
            text = "다른 참여자가 있거나 이미 시작해서 바꿀 수 없어요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!enabled) Text("🔒", style = RuleUpTheme.typography.caption)
            Text(label, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
        }
        GradientSwitch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
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
private fun StepButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(RuleUpTheme.shapes.pill)
                .background(RuleUpTheme.colors.surfaceVariant)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyBold)
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

private fun Tier.label(): String =
    when (this) {
        Tier.BRONZE -> "브론즈"
        Tier.SILVER -> "실버"
        Tier.GOLD -> "골드"
        Tier.DIAMOND -> "다이아"
        Tier.RUBY -> "루비"
    }

private const val SECONDS_PER_MINUTE = 60
