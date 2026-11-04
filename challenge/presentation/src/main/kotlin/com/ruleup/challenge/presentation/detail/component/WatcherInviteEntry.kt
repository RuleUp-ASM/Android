package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.presentation.R
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.detail.viewmodel.WatcherInviteSheet
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.remoteconfig.domain.WatcherInviteUi

@Composable
internal fun WatcherInviteEntry(
    state: ChallengeDetailState,
    onIntent: (ChallengeDetailIntent) -> Unit,
) {
    val detail = state.detail ?: return
    val ui = state.watcherInviteUi
    if (ui == WatcherInviteUi.LEGACY) return
    val watchers = state.watchers?.watchers ?: if (detail.penalties?.watcher == false) emptyList() else return
    val accepted = watchers.count { it.status == WatcherStatus.CONSENTED || it.status == WatcherStatus.ACTIVE }
    val pending = watchers.any { it.status == WatcherStatus.INVITED }
    val isCard = ui == WatcherInviteUi.A
    val colors = RuleUpTheme.colors
    val spacing = RuleUpTheme.spacing
    val title: String
    val description: String
    val icon: Int
    val iconBackground: Color
    val action: String
    when {
        accepted > 0 -> {
            title = "응원 친구 ${accepted}명"
            description = "실패가 확정되면 응원 친구에게 알려요."
            icon = R.drawable.ic_watcher_invite_active
            iconBackground = colors.successContainer
            action = "응원 친구 관리"
        }

        pending -> {
            title = "친구 수락 대기 중"
            description = "친구가 수락하면 응원 친구로 연결돼요."
            icon = R.drawable.ic_watcher_invite_pending
            iconBackground = colors.warningContainer
            action = "초대 상태 보기"
        }

        else -> {
            title = if (isCard) "친구에게 응원 부탁하기" else "응원 친구 초대"
            description =
                if (isCard) {
                    "루틴 실패가 확정되면 초대한 친구에게 알려요."
                } else {
                    "실패가 확정되면 친구에게 알려요."
                }
            icon = R.drawable.ic_watcher_invite_empty
            iconBackground = colors.brandSoft
            action = "친구 초대"
        }
    }
    val onOpen = { onIntent(ChallengeDetailIntent.OpenWatcherInvite) }
    var hasReported by remember(detail.challengeId, ui) { mutableStateOf(false) }
    val shape = RuleUpTheme.shapes.large
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.lg, vertical = spacing.sm)
                .onGloballyPositioned {
                    if (!hasReported && !it.boundsInWindow().isEmpty) {
                        hasReported = true
                        onIntent(ChallengeDetailIntent.WatcherInviteShown)
                    }
                }.clip(shape)
                .background(colors.surface)
                .border(1.dp, colors.border, shape)
                .then(if (isCard) Modifier else Modifier.singleClickable(onClick = onOpen))
                .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = Color.Unspecified)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = RuleUpTheme.typography.cardTitle, color = colors.textPrimary)
                Text(description, style = RuleUpTheme.typography.caption, color = colors.textSecondary)
            }
            if (!isCard) {
                Icon(painterResource(R.drawable.ic_watcher_invite_chevron), contentDescription = null, tint = Color.Unspecified)
            }
        }
        if (isCard) RuleUpPrimaryButton(text = action, onClick = onOpen)
    }

    state.watcherInviteSheet?.let { sheet ->
        WatcherInvitePrompt(
            sheet = sheet,
            challengeTitle = detail.title,
            justCompleted = state.watcherSetupJustCompleted,
            busy = state.isInvitingWatcher,
            onIntent = onIntent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatcherInvitePrompt(
    sheet: WatcherInviteSheet,
    challengeTitle: String,
    justCompleted: Boolean,
    busy: Boolean,
    onIntent: (ChallengeDetailIntent) -> Unit,
) {
    val share = sheet == WatcherInviteSheet.SHARE
    val colors = RuleUpTheme.colors
    val spacing = RuleUpTheme.spacing
    val type = RuleUpTheme.typography
    val onDismiss = { if (!busy) onIntent(ChallengeDetailIntent.DismissWatcherInvite) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xl, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            if (share || justCompleted) {
                Text(
                    text = if (share) challengeTitle else "인증 준비 완료",
                    color = colors.brandStrong,
                    style = type.caption,
                )
            }
            Text("친구에게 응원 부탁하기", color = colors.textPrimary, style = type.section)
            Text(
                text =
                    if (share) {
                        "초대 링크를 보내기 전에 친구가 받는 내용을 확인해 주세요."
                    } else {
                        "혼자 지키기 어려운 날엔 친구에게 도움을 부탁해 보세요."
                    },
                color = colors.textSecondary,
                style = type.body,
            )
            if (share) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.surfaceVariant, RuleUpTheme.shapes.medium)
                            .padding(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.xxs),
                ) {
                    Text("친구가 받는 내용", color = colors.textPrimary, style = type.cardTitle)
                    Text("루틴 이름과 실패 확정 사실", color = colors.textSecondary, style = type.body)
                    Text("친구가 초대를 수락하면 알림이 시작돼요.", color = colors.textSecondary, style = type.caption)
                }
            }
            RuleUpPrimaryButton(text = if (share) "카카오톡으로 초대 링크 공유" else "친구 초대하기", enabled = !busy) {
                onIntent(if (share) ChallengeDetailIntent.ConfirmWatcherInvite else ChallengeDetailIntent.OpenWatcherInvite)
            }
            val shape = RuleUpTheme.shapes.medium
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(1.dp, colors.borderStrong, shape)
                        .singleClickable(enabled = !busy, onClick = onDismiss)
                        .padding(vertical = spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text("나중에", color = colors.textSecondary, style = type.bodyMedium)
            }
        }
    }
}
