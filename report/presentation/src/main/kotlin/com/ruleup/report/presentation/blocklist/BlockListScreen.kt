package com.ruleup.report.presentation.blocklist

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.BlockedChallenge
import com.ruleup.report.domain.entity.BlockedUser
import com.ruleup.report.presentation.blocklist.viewmodel.BlockListIntent
import com.ruleup.report.presentation.blocklist.viewmodel.BlockListState
import com.ruleup.report.presentation.blocklist.viewmodel.BlockListViewModel
import com.ruleup.report.presentation.blocklist.viewmodel.BlockTarget
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.ttiContentDrawn

/** 신고한 사용자·챌린지. */
@Composable
fun BlockListScreen(
    modifier: Modifier = Modifier,
    viewModel: BlockListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    LaunchedEffect(Unit) {
        viewModel.onIntent(BlockListIntent.Load)
    }

    BlockListContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
internal fun BlockListContent(
    state: BlockListState,
    onIntent: (BlockListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "신고한 사용자 · 챌린지", onBack = { onIntent(BlockListIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.errorMessage != null && state.isEmpty ->
                ErrorBody(message = state.errorMessage, onRetry = { onIntent(BlockListIntent.Retry) })

            state.isEmpty -> EmptyBody()

            else -> BlockBody(state = state, onIntent = onIntent)
        }
    }

    state.confirming?.let { target ->
        UnblockConfirmSheet(
            target = target,
            submitting = state.unblocking,
            onConfirm = { onIntent(BlockListIntent.Unblock) },
            onDismiss = { onIntent(BlockListIntent.DismissConfirm) },
        )
    }
}

@Composable
private fun ErrorBody(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).ttiContentDrawn(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.padding(6.dp))
        RuleUpPrimaryButton(text = "다시 시도", onClick = onRetry)
    }
}

/** Figma `1287:2`. */
@Composable
private fun EmptyBody() {
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).ttiContentDrawn(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "아직 차단한 대상이 없어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
        )
        Spacer(Modifier.padding(3.dp))
        Text(
            text = "신고하면 그 사용자와 챌린지가 여기에 쌓여요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BlockBody(
    state: BlockListState,
    onIntent: (BlockListIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().ttiContentDrawn(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (state.blocks.users.isNotEmpty()) {
            item { SectionLabel("사용자") }
            // 두 목록이 한 LazyColumn 을 공유한다.
            items(state.blocks.users, key = { "u-${it.userId}" }) { user ->
                BlockRow(
                    title = user.maskedNickname.ifBlank { "이름이 가려진 사용자" },
                    subtitle = user.subtitle(),
                    onUnblock = {
                        onIntent(
                            BlockListIntent.ConfirmUnblock(
                                BlockTarget.User(user.userId, user.maskedNickname),
                            ),
                        )
                    },
                )
            }
        }

        if (state.blocks.challenges.isNotEmpty()) {
            item { SectionLabel("챌린지") }
            items(state.blocks.challenges, key = { "c-${it.challengeId}" }) { challenge ->
                BlockRow(
                    title = challenge.maskedTitle.ifBlank { "가려진 챌린지" },
                    subtitle = challenge.subtitle(),
                    onUnblock = {
                        onIntent(
                            BlockListIntent.ConfirmUnblock(
                                BlockTarget.Challenge(challenge.challengeId, challenge.maskedTitle),
                            ),
                        )
                    },
                )
            }
        }

        item { UnblockNotice() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.smallBold,
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun BlockRow(
    title: String,
    subtitle: String,
    onUnblock: () -> Unit,
) {
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
            Text(text = title, color = colors.textPrimary, style = RuleUpTheme.typography.bodyBold, maxLines = 1)
            Text(text = subtitle, color = colors.textMuted, style = RuleUpTheme.typography.caption, maxLines = 1)
        }
        Spacer(Modifier.width(11.dp))
        Text(
            text = "차단 해제",
            color = colors.textSecondary,
            style = RuleUpTheme.typography.caption,
            modifier =
                Modifier
                    .clip(RuleUpTheme.shapes.small)
                    .border(1.dp, colors.border, RuleUpTheme.shapes.small)
                    .singleClickable(onClick = onUnblock)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

/** 해제를 신고 취소로 오해하지 않게 못 박는다 */
@Composable
private fun UnblockNotice() {
    Text(
        text = "차단을 풀어도 신고 기록은 남아요",
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.caption,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

/** "참여 중 · 8.30 차단". */
internal fun BlockedChallenge.subtitle(): String =
    listOfNotNull(
        "참여 중".takeIf { participating },
        blockedAt?.let { blockedLabel(it) },
    ).joinToString(" · ").ifBlank { "차단됨" }

internal fun BlockedUser.subtitle(): String = blockedAt?.let { blockedLabel(it) } ?: "차단됨"

/** "8.30 차단". */
internal fun blockedLabel(iso: String): String {
    val parts = iso.substringBefore('T').split('-')
    if (parts.size != 3) return "차단됨"
    val month = parts[1].toIntOrNull() ?: return "차단됨"
    val day = parts[2].toIntOrNull() ?: return "차단됨"
    return "$month.$day 차단"
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun BlockListContentPreview() {
    RuleUpTheme {
        BlockListContent(
            state =
                com.ruleup.report.presentation.blocklist.viewmodel.BlockListState.initial
                    .copy(isLoading = false),
            onIntent = { },
        )
    }
}
