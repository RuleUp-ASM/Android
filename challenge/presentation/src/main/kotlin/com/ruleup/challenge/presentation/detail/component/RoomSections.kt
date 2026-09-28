package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeMember
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.challenge.presentation.common.capacityLabel
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import java.util.Locale

// 방 홈(그룹 챌린지 ACTIVE 멤버 전용 — GET room 성공 시 상세에 확장 렌더링)에서만 쓰인다.

/** 성공률 0~1 → 표시용 백분율. */
internal fun Double.toPercentText(): String {
    val percent = this * 100
    return if (percent % 1.0 == 0.0) {
        percent.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", percent)
    }
}

/** 나가기는 방장을 포함한 모두에게 연다 */
@Composable
internal fun RoomMemberSection(
    members: List<ChallengeMember>,
    participantCount: Int,
    // null 이면 무제한
    maxParticipants: Int?,
    myUserId: String?,
    actionEnabled: Boolean,
    // 비공개 그룹 방의 방장만
    canInviteMember: Boolean,
    onInviteMember: () -> Unit,
    onLeave: () -> Unit,
    onReportMember: (String) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
) {
    RuleUpCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("멤버")
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$participantCount / ${capacityLabel(maxParticipants)}",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallMedium,
            )
        }

        members.forEach { member ->
            MemberRow(
                member = member,
                // 나 자신은 신고할 수 없다
                onReport = { onReportMember(member.userId) }.takeIf { member.userId != myUserId },
                // 내 프로필은 마이페이지가 원본이라 여기서 열지 않는다.
                onOpenProfile = { onOpenProfile(member.userId) }.takeIf { member.userId != myUserId },
            )
        }

        if (canInviteMember) {
            RuleUpPrimaryButton(
                text = "초대 링크 공유",
                enabled = actionEnabled,
                onClick = onInviteMember,
            )
            Text(
                text = "비공개 방은 초대 링크로만 들어올 수 있어요 · 링크는 7일 뒤 만료돼요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }

        DangerActionButton(
            text = "챌린지 나가기",
            enabled = actionEnabled,
            onClick = onLeave,
        )
    }
}

@Composable
private fun MemberRow(
    member: ChallengeMember,
    onReport: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .let { base -> onOpenProfile?.let { base.singleClickable(onClick = it) } ?: base },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.nickname.take(1),
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = member.nickname,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        RoleBadge(member.role)
        Spacer(Modifier.width(8.dp))
        member.tier?.let { tier ->
            Text(
                text = tier.value,
                color = RuleUpPalette.StatusWarn,
                style = RuleUpTheme.typography.smallBold,
            )
        }
        onReport?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "신고",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.smallMedium,
                modifier = Modifier.singleClickable(globalGuard = false, onClick = it),
            )
        }
    }
}

@Composable
private fun RoleBadge(role: MemberRole) {
    val label =
        when (role) {
            MemberRole.OWNER -> "방장"
            MemberRole.MANAGER -> "관리자"
            else -> return
        }
    val color = if (role.isOwner) RuleUpTheme.colors.brand else RuleUpTheme.colors.textSlate
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = color, style = RuleUpTheme.typography.tinyBold)
    }
}

@Composable
private fun DangerActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val color = if (enabled) RuleUpTheme.colors.danger else RuleUpTheme.colors.textMuted
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, color, RoundedCornerShape(12.dp))
                .then(if (enabled) Modifier.singleClickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = color, style = RuleUpTheme.typography.bodyBold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textPrimary,
        style = RuleUpTheme.typography.cardTitle,
    )
}

@Composable
private fun Dot(color: Color) {
    Box(
        modifier =
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
    )
}

/** 이 방의 알림 음소거 (알림 3계층의 ③). */
@Composable
internal fun RoomMuteSection(
    muted: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "이 챌린지 알림 끄기",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(
                text = "푸시만 멈춰요 · 알림함에는 그대로 쌓여요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Switch(
            checked = muted,
            onCheckedChange = onToggle,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = RuleUpTheme.colors.brand),
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomMemberSectionPreview() {
    RuleUpTheme {
        RoomMemberSection(
            members = emptyList(),
            participantCount = 1,
            maxParticipants = null,
            myUserId = null,
            actionEnabled = true,
            canInviteMember = true,
            onInviteMember = {},
            onLeave = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RoomMuteSectionPreview() {
    RuleUpTheme {
        RoomMuteSection(muted = true, enabled = true, onToggle = { })
    }
}
