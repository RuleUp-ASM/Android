package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.ChallengeMember
import com.ruleup.challenge.domain.entity.MemberRole
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import java.util.Locale

// 방 홈(그룹 챌린지 ACTIVE 멤버 전용 — GET room 성공 시 상세에 확장 렌더링)에서만 쓰인다.

/** 성공률 0~1 → 표시용 백분율. 0.92 → "92", 0.925 → "92.5". */
internal fun Double.toPercentText(): String {
    val percent = this * 100
    return if (percent % 1.0 == 0.0) {
        percent.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", percent)
    }
}

/**
 * 탈퇴·삭제 노출 조건은 서버 규칙과 같다 — 삭제는 방장 + 참여자(본인 제외) 0명일 때만이다.
 * 디자인 시안이 없어 방 홈 섹션 카드 컨벤션을 따른다.
 */
@Composable
internal fun RoomMemberSection(
    members: List<ChallengeMember>,
    participantCount: Int,
    maxParticipants: Int,
    myRole: MemberRole,
    myUserId: String?,
    actionEnabled: Boolean,
    delegationBanner: String?,
    onLeave: () -> Unit,
    onDelete: () -> Unit,
    onPromote: (String) -> Unit,
    onDemote: (String) -> Unit,
    onRequestDelegation: (String) -> Unit,
    onCancelDelegation: () -> Unit,
) {
    RuleUpCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("멤버")
            Spacer(Modifier.width(6.dp))
            Text(
                text = "$participantCount / $maxParticipants",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallMedium,
            )
        }

        if (delegationBanner != null) {
            DelegationBanner(text = delegationBanner, enabled = actionEnabled, onCancel = onCancelDelegation)
        }

        members.forEach { member ->
            MemberRow(
                member = member,
                ownerManage = myRole.isOwner && !member.role.isOwner,
                selfDemote = myRole.isManager && member.role.isManager && member.userId == myUserId,
                actionEnabled = actionEnabled,
                onPromote = { onPromote(member.userId) },
                onDemote = { onDemote(member.userId) },
                onRequestDelegation = { onRequestDelegation(member.userId) },
            )
        }

        when {
            !myRole.isOwner ->
                DangerActionButton(
                    text = "챌린지 나가기",
                    enabled = actionEnabled,
                    onClick = onLeave,
                )
            // 방장: 참여자(본인 제외)가 없을 때만 삭제 가능.
            participantCount <= 1 ->
                DangerActionButton(
                    text = "챌린지 삭제",
                    enabled = actionEnabled,
                    onClick = onDelete,
                )
            else ->
                Text(
                    text = "참여자가 있는 동안에는 삭제할 수 없어요. 방장 위임 후 나갈 수 있어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
        }
    }
}

@Composable
private fun MemberRow(
    member: ChallengeMember,
    ownerManage: Boolean,
    selfDemote: Boolean,
    actionEnabled: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRequestDelegation: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
        if (ownerManage || selfDemote) {
            Spacer(Modifier.width(4.dp))
            MemberManageMenu(
                role = member.role,
                selfDemote = selfDemote,
                enabled = actionEnabled,
                onPromote = onPromote,
                onDemote = onDemote,
                onRequestDelegation = onRequestDelegation,
            )
        }
    }
}

@Composable
private fun MemberManageMenu(
    role: MemberRole,
    selfDemote: Boolean,
    enabled: Boolean,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRequestDelegation: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .then(if (enabled) Modifier.singleClickable(globalGuard = false) { expanded = true } else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text("⋯", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.section)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (selfDemote) {
                DropdownMenuItem(
                    text = { Text("관리자 그만두기") },
                    onClick = {
                        expanded = false
                        onDemote()
                    },
                )
            } else {
                when (role) {
                    MemberRole.MEMBER ->
                        DropdownMenuItem(
                            text = { Text("공동 관리자 임명") },
                            onClick = {
                                expanded = false
                                onPromote()
                            },
                        )

                    MemberRole.MANAGER -> {
                        DropdownMenuItem(
                            text = { Text("공동 관리자 해제") },
                            onClick = {
                                expanded = false
                                onDemote()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("방장 위임") },
                            onClick = {
                                expanded = false
                                onRequestDelegation()
                            },
                        )
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun DelegationBanner(
    text: String,
    enabled: Boolean,
    onCancel: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.brandSoft)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = RuleUpTheme.colors.brandStrong,
            style = RuleUpTheme.typography.smallMedium,
            modifier = Modifier.weight(1f),
        )
        if (enabled) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "취소",
                color = RuleUpTheme.colors.danger,
                style = RuleUpTheme.typography.smallBold,
                modifier = Modifier.singleClickable(globalGuard = false) { onCancel() },
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
