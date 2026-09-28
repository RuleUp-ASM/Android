package com.ruleup.verification.presentation.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.verification.domain.entity.ManualNoteLimits
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitIntent
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitState
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 수동 인증 제출. */
@Composable
fun ManualSubmitScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ManualSubmitViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    LaunchedEffect(challengeId) { viewModel.onIntent(ManualSubmitIntent.Load(challengeId)) }
    ManualSubmitContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun ManualSubmitContent(
    state: ManualSubmitState,
    onIntent: (ManualSubmitIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
                .imePadding(),
    ) {
        RuleUpTopBar(title = "오늘 인증", onBack = { onIntent(ManualSubmitIntent.Back) })

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.isLoading) {
                LoadingBlock()
            } else if (state.loadFailed) {
                ErrorBlock(message = state.errorMessage.orEmpty(), onIntent = onIntent)
            } else {
                ChallengeCard(title = state.title, date = state.date, window = state.window)
                TodayStatusCard(checked = state.checked, notTarget = state.notTarget)
                if (state.streakAfter != null) {
                    StreakCard(days = state.streakAfter)
                }
                NoteCard(
                    note = state.note,
                    // 인증 완료 후 메모 편집 잠금.
                    enabled = !state.checked && !state.notTarget,
                    onChange = { onIntent(ManualSubmitIntent.NoteChanged(it)) },
                )
                // 인증 전 점수 미반영 안내.
                Text(
                    text = "수동 인증은 점수에 반영되지 않아요",
                    style = RuleUpTheme.typography.caption,
                    color = colors.textMuted,
                )
                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage,
                        style = RuleUpTheme.typography.smallMedium,
                        color = colors.danger,
                    )
                }
            }
        }

        // 하단 고정 인증 버튼.
        if (!state.isLoading && !state.loadFailed && !state.notTarget) {
            SubmitBar(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun LoadingBlock() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = RuleUpTheme.colors.brand)
    }
}

@Composable
private fun ErrorBlock(
    message: String,
    onIntent: (ManualSubmitIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = message,
            style = RuleUpTheme.typography.body,
            color = RuleUpTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        OutlineButton(text = "다시 시도", enabled = true) { onIntent(ManualSubmitIntent.Retry) }
    }
}

@Composable
private fun ChallengeCard(
    title: String,
    date: String,
    window: String?,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.card)
                .background(colors.surface)
                .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "수동 인증",
            style = RuleUpTheme.typography.captionBold,
            color = colors.brand,
            modifier =
                Modifier
                    .clip(RuleUpTheme.shapes.pill)
                    .background(colors.brandSoft)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
        )
        // 제목 조회 실패 시 기본 문구.
        Text(
            text = title.ifBlank { "오늘 인증" },
            style = RuleUpTheme.typography.title,
            color = colors.textPrimary,
        )
        Text(
            text = listOfNotNull(koreanDate(date), window).joinToString(" · "),
            style = RuleUpTheme.typography.small,
            color = colors.textMuted,
        )
    }
}

@Composable
private fun TodayStatusCard(
    checked: Boolean,
    notTarget: Boolean,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.card)
                .background(colors.surface)
                .padding(vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(if (checked) colors.success else colors.background)
                    .border(
                        width = 2.dp,
                        color = if (checked) colors.success else colors.border,
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✓",
                style = RuleUpTheme.typography.numberM,
                color = if (checked) Color.White else colors.textMuted,
            )
        }
        Text(
            text =
                when {
                    notTarget -> "오늘은 인증하는 날이 아니에요"
                    checked -> "오늘 인증을 마쳤어요"
                    else -> "아직 인증 전이에요"
                },
            style = RuleUpTheme.typography.cardTitle,
            color = colors.textPrimary,
        )
    }
}

@Composable
private fun StreakCard(days: Int) {
    val colors = RuleUpTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.card)
                .background(colors.surface)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "연속",
            style = RuleUpTheme.typography.small,
            color = colors.textMuted,
            modifier = Modifier.weight(1f),
        )
        Text(text = "${days}일", style = RuleUpTheme.typography.numberS, color = colors.textPrimary)
    }
}

@Composable
private fun NoteCard(
    note: String,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.card)
                .background(colors.surface)
                .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "메모 (선택)", style = RuleUpTheme.typography.smallBold, color = colors.textPrimary)
        BasicTextField(
            value = note,
            // 메모 길이 제한.
            onValueChange = { if (it.length <= ManualNoteLimits.MAX_LENGTH) onChange(it) },
            enabled = enabled,
            textStyle = RuleUpTheme.typography.small.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.brand),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
        if (note.isEmpty()) {
            Text(
                text = "오늘 무엇을 했는지 남겨 두면 나중에 돌아볼 수 있어요",
                style = RuleUpTheme.typography.caption,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun SubmitBar(
    state: ManualSubmitState,
    onIntent: (ManualSubmitIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.background)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        if (state.checked) {
            // 체크된 날의 주 동작은 되돌리기 하나뿐이다.
            OutlineButton(text = "체크 해제", enabled = state.canUncheck) {
                onIntent(ManualSubmitIntent.Uncheck)
            }
        } else {
            RuleUpPrimaryButton(
                text = "오늘 완료로 체크",
                enabled = state.canSubmit,
                onClick = { onIntent(ManualSubmitIntent.Submit) },
            )
        }
    }
}

@Composable
private fun OutlineButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface)
                .border(width = 1.dp, color = colors.border, shape = RuleUpTheme.shapes.medium)
                .singleClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = RuleUpTheme.typography.cardTitle,
            color = if (enabled) colors.textPrimary else colors.textMuted,
        )
    }
}

/** `2026-09-14` → `9월 14일 월요일`. */
private fun koreanDate(raw: String): String? =
    runCatching { LocalDate.parse(raw) }
        .getOrNull()
        ?.let { date ->
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            "${date.monthValue}월 ${date.dayOfMonth}일 $dayOfWeek"
        }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ManualSubmitContentPreview() {
    RuleUpTheme {
        ManualSubmitContent(
            state =
                com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitState(
                    challengeId = "미리보기",
                    isLoading = false,
                    title = "매일 꾸준히 걷기",
                    date = "2026-09-28",
                    window = null,
                    status = null,
                    verificationId = null,
                    streakAfter = null,
                    note = "미리보기",
                    isSubmitting = false,
                    errorMessage = null,
                ),
            onIntent = {
            },
        )
    }
}
