package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.Watcher
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/** 내 감시자 관리. */
@Composable
internal fun WatcherSection(
    watchers: List<Watcher>,
    limit: Int?,
    isInviting: Boolean,
    onInvite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "감시자",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.cardTitle,
            )
            val activeCount = watchers.count { it.status.isActive }
            Text(
                // 무료 한도 대비 현재 유효(만료·해제 제외) 감시자 수.
                text = limit?.let { "$activeCount/$it" } ?: "${activeCount}명",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallMedium,
            )
        }
        Text(
            text = "내가 루틴 인증에 실패하면 감시자에게 알림이 가요. 초대는 내 카카오톡으로 직접 보내요.",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.caption,
        )
        if (watchers.isEmpty()) {
            Text(
                text = "아직 감시자가 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                watchers.forEach { watcher ->
                    WatcherRow(watcher = watcher)
                }
            }
        }
        InviteButton(isInviting = isInviting, onClick = onInvite)
    }
}

@Composable
private fun WatcherRow(watcher: Watcher) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_person),
                contentDescription = null,
                tint = RuleUpTheme.colors.brand,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = watcher.shownName,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
            modifier = Modifier.weight(1f),
        )
        WatcherStatusBadge(status = watcher.status)
    }
}

@Composable
private fun WatcherStatusBadge(status: WatcherStatus) {
    val (label, color) =
        when (status) {
            WatcherStatus.INVITED -> "수락 대기" to RuleUpTheme.colors.warning
            WatcherStatus.CONSENTED, WatcherStatus.ACTIVE -> "감시 중" to RuleUpTheme.colors.success
            WatcherStatus.REVOKED -> "해제됨" to RuleUpTheme.colors.textMuted
            WatcherStatus.EXPIRED -> "만료" to RuleUpTheme.colors.textMuted
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            color = color,
            style = RuleUpTheme.typography.tinyBold,
        )
    }
}

@Composable
private fun InviteButton(
    isInviting: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.brand)
                .singleClickable(enabled = !isInviting, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isInviting) "초대 만드는 중..." else "카카오톡으로 감시자 초대하기",
            color = Color.White,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun WatcherSectionPreview() {
    RuleUpTheme {
        WatcherSection(watchers = emptyList(), limit = null, isInviting = false, onInvite = { })
    }
}
