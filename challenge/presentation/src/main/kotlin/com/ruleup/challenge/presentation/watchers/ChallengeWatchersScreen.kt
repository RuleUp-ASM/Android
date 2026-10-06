package com.ruleup.challenge.presentation.watchers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.challenge.domain.entity.ChallengeWatchers
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.WatcherType
import com.ruleup.challenge.presentation.watcher.WatcherInviteSharer
import com.ruleup.challenge.presentation.watchers.viewmodel.ChallengeWatchersEffect
import com.ruleup.challenge.presentation.watchers.viewmodel.ChallengeWatchersIntent
import com.ruleup.challenge.presentation.watchers.viewmodel.ChallengeWatchersState
import com.ruleup.challenge.presentation.watchers.viewmodel.ChallengeWatchersViewModel
import com.ruleup.designsystem.R
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.ui.helper.LocalMessageHelper

/** 내 감시자 관리(Figma 1134:1603). */
@Composable
fun ChallengeWatchersScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: ChallengeWatchersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
    val context = LocalContext.current
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(challengeId) { viewModel.onIntent(ChallengeWatchersIntent.Load(challengeId)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChallengeWatchersEffect.ShowMessage -> messageHelper.showToast(effect.message)
                is ChallengeWatchersEffect.ShareInvite -> {
                    val shared = WatcherInviteSharer.share(context = context, card = effect.card, inviteUrl = effect.inviteUrl)
                    if (!shared) messageHelper.showToast("카카오톡 공유를 열지 못했어요")
                }
            }
        }
    }

    ChallengeWatchersContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun ChallengeWatchersContent(
    state: ChallengeWatchersState,
    onIntent: (ChallengeWatchersIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "감시자", onBack = { onIntent(ChallengeWatchersIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.errorMessage != null -> ErrorBody(message = state.errorMessage, onRetry = { onIntent(ChallengeWatchersIntent.Retry) })

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = listOfNotNull(state.challengeTitle, "실패가 확정된 날에만 감시자에게 알림이 가요").joinToString(" · "),
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.smallMedium,
                    )
                    val watchers = state.watchers
                    if (!state.watcherEnabled || watchers == null) {
                        DisabledNotice()
                    } else {
                        WatcherList(watchers = watchers.watchers)
                        InviteButton(
                            remaining = watchers.remaining,
                            isInviting = state.isInviting,
                            onClick = { onIntent(ChallengeWatchersIntent.Invite) },
                        )
                        Text(
                            text = "상대가 수락해야 감시자가 돼요 · 카카오톡으로 초대장이 가요",
                            color = RuleUpTheme.colors.textMuted,
                            style = RuleUpTheme.typography.caption,
                        )
                    }
                }
        }
    }
}

@Composable
private fun WatcherList(watchers: List<Watcher>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp)),
    ) {
        if (watchers.isEmpty()) {
            Text(
                text = "아직 감시자가 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
        watchers.forEachIndexed { index, watcher ->
            if (index > 0) HorizontalDivider(thickness = 1.dp, color = RuleUpTheme.colors.background)
            WatcherRow(watcher = watcher)
        }
    }
}

@Composable
private fun WatcherRow(watcher: Watcher) {
    val accepted = watcher.status == WatcherStatus.CONSENTED || watcher.status == WatcherStatus.ACTIVE
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (accepted) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = watcher.shownName.take(1),
                color = if (accepted) RuleUpTheme.colors.brand else RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(text = watcher.shownName, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyBold)
            val (label, color) = watcher.status.label()
            Text(text = label, color = color, style = RuleUpTheme.typography.captionMedium)
        }
    }
}

@Composable
private fun WatcherStatus.label(): Pair<String, Color> =
    when (this) {
        WatcherStatus.INVITED -> "수락 기다리는 중" to RuleUpTheme.colors.warning
        WatcherStatus.CONSENTED, WatcherStatus.ACTIVE -> "수락했어요" to RuleUpTheme.colors.success
        WatcherStatus.REVOKED -> "해제됐어요" to RuleUpTheme.colors.textMuted
        WatcherStatus.EXPIRED -> "초대가 만료됐어요" to RuleUpTheme.colors.textMuted
    }

@Composable
private fun InviteButton(
    remaining: Int?,
    isInviting: Boolean,
    onClick: () -> Unit,
) {
    val lineColor = RuleUpTheme.colors.border
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .drawBehind {
                    drawRoundRect(
                        color = lineColor,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                    )
                }.singleClickable(enabled = !isInviting, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = null,
            tint = RuleUpTheme.colors.brand,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text =
                when {
                    isInviting -> "초대 만드는 중..."
                    remaining != null && remaining > 0 -> "감시자 추가 (${remaining}명 더 가능)"
                    else -> "감시자 추가"
                },
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DisabledNotice() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = "이 챌린지는 감시자를 쓰지 않아요", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
        Text(
            text = "방장이 챌린지 설정에서 감시자 벌칙을 켜면 감시자를 초대할 수 있어요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
    }
}

@Composable
private fun ErrorBody(
    message: String,
    onRetry: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = message, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.labelMedium)
            Text(
                text = "다시 시도",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.bodyBold,
                modifier = Modifier.singleClickable(onClick = onRetry),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ChallengeWatchersContentPreview() {
    RuleUpTheme {
        ChallengeWatchersContent(
            state =
                ChallengeWatchersState.initial.copy(
                    isLoading = false,
                    challengeTitle = "아침 6:30 기상",
                    watcherEnabled = true,
                    watchers =
                        ChallengeWatchers(
                            limit = 3,
                            watchers =
                                listOf(
                                    previewWatcher("엄마", WatcherStatus.ACTIVE),
                                    previewWatcher("동생", WatcherStatus.INVITED),
                                ),
                        ),
                ),
            onIntent = {},
        )
    }
}

private fun previewWatcher(
    name: String,
    status: WatcherStatus,
) = Watcher(
    watcherId = name,
    type = WatcherType.entries.first(),
    channel = null,
    status = status,
    displayName = name,
    contactMasked = null,
    expiresAt = null,
    reinviteAvailableAt = null,
)
