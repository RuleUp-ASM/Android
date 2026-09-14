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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.ManualNoteLimits
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitIntent
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitState
import com.ruleup.verification.presentation.manual.viewmodel.ManualSubmitViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * 수동 인증 제출 (Figma `1443:2`).
 *
 * **솔로·그룹이 같은 화면을 쓴다.** 체크 CTA 가 방 정보 탭 안에만 있던 동안, 방 홈을 받지 않는
 * 솔로 수동 방은 오늘 인증을 할 방법이 아예 없었다.
 *
 * [challengeId] 는 라우트 인자로 받는다 — 이 내비게이션은 `SavedStateHandle` 을 채우지 않으므로
 * ViewModel 이 인자를 직접 읽을 수 없다.
 */
@Composable
fun ManualSubmitScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ManualSubmitViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                    // 체크가 끝난 뒤 메모를 고쳐도 서버에 갈 곳이 없다 — 고칠 수 있는 것처럼 보이지 않게 막는다.
                    enabled = !state.checked && !state.notTarget,
                    onChange = { onIntent(ManualSubmitIntent.NoteChanged(it)) },
                )
                // 체크 전에도 보여야 한다 — 점수가 오를 줄 알고 누르는 일을 막는 것이 이 문구의 목적이다.
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

        // CTA 는 스크롤 밖에 고정한다 — 메모를 길게 쓴 뒤 체크를 찾아 내려가게 만들지 않는다.
        // 대상일이 아닌 날은 아예 두지 않는다 — 누를 수 없는 버튼은 이유를 두 번 말할 뿐이다.
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
        // 제목 조회가 실패해도 화면은 선다 — 그때 빈 줄을 남기지 않는다.
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
            // 길이는 여기서 막는다 — 서버가 검증하지 않으므로 ViewModel 이 잘라 내면 사용자가
            // 사라진 글자를 보게 된다.
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
            // 체크된 날의 주 동작은 되돌리기 하나뿐이다. 강조색을 주지 않는다.
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

/**
 * `2026-09-14` → `9월 14일 월요일`. 서버가 준 날(KST)을 그대로 옮긴다 — 기기 시계로 다시 계산하면
 * 자정 근처에서 하루가 어긋난다. 형식이 어긋난 값은 표기를 생략한다.
 */
private fun koreanDate(raw: String): String? =
    runCatching { LocalDate.parse(raw) }
        .getOrNull()
        ?.let { date ->
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.KOREAN)
            "${date.monthValue}월 ${date.dayOfMonth}일 $dayOfWeek"
        }
