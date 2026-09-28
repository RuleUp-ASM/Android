package com.ruleup.profile.presentation.appeals

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.presentation.appeals.viewmodel.MyAppealsIntent
import com.ruleup.profile.presentation.appeals.viewmodel.MyAppealsViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.verification.domain.entity.AppealHistoryItem

/** 이의 내역. */
@Composable
fun MyAppealsScreen(
    modifier: Modifier = Modifier,
    viewModel: MyAppealsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    LaunchedEffect(Unit) {
        viewModel.onIntent(MyAppealsIntent.Load)
    }

    MyAppealsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun MyAppealsContent(
    state: com.ruleup.profile.presentation.appeals.viewmodel.MyAppealsState,
    onIntent: (MyAppealsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "이의 내역", onBack = { onIntent(MyAppealsIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.errorMessage != null ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.body,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.padding(6.dp))
                    RuleUpPrimaryButton(
                        text = "다시 시도",
                        onClick = { onIntent(MyAppealsIntent.Retry) },
                    )
                }

            state.history.isEmpty() ->
                Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "아직 낸 이의가 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.body,
                    )
                }

            else ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.history, key = { it.appealId }) { AppealRow(it) }
                    item { AutoAcceptNotice() }
                }
        }
    }
}

@Composable
private fun AppealRow(item: AppealHistoryItem) {
    val colors = RuleUpTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface)
                .border(1.dp, colors.border, RuleUpTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = item.rowTitle(),
                color = colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
            )
            if (item.reason.isNotBlank()) {
                Text(
                    text = "\"${item.reason}\"",
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        // 접수된 이의는 전부 인용이다
        StatusChip(text = "인용", tone = StatusChipTone.Success)
    }
}

/** 하단 안내. */
@Composable
private fun AutoAcceptNotice() {
    Text(
        text = "이의는 접수되면 바로 받아들여져요",
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.caption,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

/** "아침 6:30 기상 · 8.2" */
internal fun AppealHistoryItem.rowTitle(): String =
    listOf(routineTitle, appealDateLabel(date))
        .filter { it.isNotBlank() }
        .joinToString(" · ")

/** 신청일 "8.2". */
internal fun appealDateLabel(iso: String): String {
    val parts = iso.substringBefore('T').split('-')
    if (parts.size != 3) return iso
    val month = parts[1].toIntOrNull() ?: return iso
    val day = parts[2].toIntOrNull() ?: return iso
    return "$month.$day"
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 390)
@Composable
private fun MyAppealsContentPreview() {
    RuleUpTheme {
        MyAppealsContent(
            state =
                com.ruleup.profile.presentation.appeals.viewmodel.MyAppealsState.initial
                    .copy(isLoading = false),
            onIntent = {},
        )
    }
}
