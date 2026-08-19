package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.RoomTopRanker
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.designsystem.component.RuleUpCard
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import java.util.Locale

// 방 홈 섹션 모음 (그룹 챌린지 ACTIVE 멤버 전용 — GET room 성공 시 상세에 확장 렌더링).
// 요약 3카드 스타일은 피그마 "챌린지 상세 그룹"(420:321) 시안, 공지·랭킹 섹션은 시안 부재로
// 기존 디자인 시스템 토큰으로 구성했다.

/** 방 성공률 · 남은 기간 · 인원 3카드 (시안 420:347~355). */
@Composable
internal fun RoomSummaryRow(summary: RoomSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoomStatCard(
            // 판정 이력이 없으면 null 이다 — 0% 로 접으면 갓 만든 방이 실패한 방처럼 보인다
            value = summary.roomSuccessRate?.let { "${it.toPercentText()}%" } ?: "–",
            label = "방 성공률",
            valueColor =
                if (summary.roomSuccessRate == null) {
                    RuleUpTheme.colors.textMuted
                } else {
                    RuleUpTheme.colors.brand
                },
            modifier = Modifier.weight(1f),
        )
        RoomStatCard(
            value = "${summary.remainingDays}일",
            label = "남은 기간",
            valueColor = RuleUpTheme.colors.success,
            modifier = Modifier.weight(1f),
        )
        RoomStatCard(
            value = "${summary.participantCount}/${summary.capacity}",
            label = "인원",
            valueColor = RuleUpPalette.StatusWarn,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 성공률 0~1 → 표시용 백분율. 0.92 → "92", 0.925 → "92.5". */
internal fun Double.toPercentText(): String {
    val percent = this * 100
    return if (percent % 1.0 == 0.0) {
        percent.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", percent)
    }
}

@Composable
private fun RoomStatCard(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            color = valueColor,
            style = RuleUpTheme.typography.title,
        )
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.captionMedium,
        )
    }
}

/**
 * 공지 섹션: 고정 공지 배너(있으면) + 목록 진입 행.
 * **읽음/미읽음 표시는 없다** — "확인해야 할 일"로 읽혀 압박이 되므로 정책상 제외됐고, 방 계약에도
 * 읽음 필드가 없다.
 */
@Composable
internal fun RoomNoticeSection(
    pinnedNotice: NoticeSummary?,
    onOpenNotices: () -> Unit,
    onOpenNotice: (String) -> Unit,
) {
    RuleUpCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .singleClickable(onClick = onOpenNotices),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("공지")
            Spacer(Modifier.weight(1f))
            Text(
                text = "전체 보기 ›",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallMedium,
            )
        }

        if (pinnedNotice != null) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RuleUpTheme.colors.brandSoft)
                        .singleClickable(onClick = { onOpenNotice(pinnedNotice.noticeId) })
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "📌", style = RuleUpTheme.typography.body)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pinnedNotice.title,
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.bodyBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Text(
                text = "등록된 고정 공지가 없어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
            )
        }
    }
}

/** 그룹 랭킹 섹션: top3 미리보기 + 전체 랭킹 진입 (시안 부재 — 랭킹 화면 시안 434:514 의 행 구성을 축약). */
@Composable
internal fun RoomRankingSection(
    topRanking: List<RoomTopRanker>,
    onOpenRanking: () -> Unit,
) {
    RuleUpCard {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .singleClickable(onClick = onOpenRanking),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("그룹 랭킹")
            Spacer(Modifier.weight(1f))
            Text(
                text = "전체 보기 ›",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.smallMedium,
            )
        }

        if (topRanking.isEmpty()) {
            Text(
                text = "아직 랭킹이 집계되지 않았어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.small,
            )
        } else {
            topRanking.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = rankEmoji(entry.rank),
                        style = RuleUpTheme.typography.labelMedium,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = entry.nickname,
                        color = RuleUpTheme.colors.textPrimary,
                        style = RuleUpTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${entry.successRate.toPercentText()}%",
                        color = RuleUpTheme.colors.brand,
                        style = RuleUpTheme.typography.bodyBold,
                    )
                }
            }
        }
    }
}

internal fun rankEmoji(rank: Int): String =
    when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "$rank"
    }

/** 내 오늘 인증 상태 (myTodayStatus). 앱이 모르는 값이면 호출부가 카드 자체를 그리지 않는다. */
@Composable
internal fun RoomTodayStatusCard(status: TodayVerificationStatus) {
    val (label, color) =
        when (status) {
            TodayVerificationStatus.DONE -> "오늘 인증 완료" to RuleUpTheme.colors.success
            // 00~03시 유예 구간 — 아직 실패가 아니다. 경고색을 쓰되 실패 문구를 쓰지 않는다.
            TodayVerificationStatus.CHECKING -> "판정 대기 중" to RuleUpPalette.StatusWarn
            TodayVerificationStatus.NOT_TARGET -> "오늘은 인증 대상일이 아니에요" to RuleUpTheme.colors.textMuted
        }
    Row(
        modifier = Modifier.ruleUpCardSurface(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle("오늘 내 상태")
        Spacer(Modifier.weight(1f))
        Dot(color)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            style = RuleUpTheme.typography.bodyBold,
        )
    }
}

/** 방장·관리자 전용 관리 진입 행(확인 대기함 등). 카드 우측 "›" 로 이동을 표시한다. */
@Composable
internal fun RoomManageEntry(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.singleClickable(onClick = onClick).ruleUpCardSurface(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(label)
        Spacer(Modifier.weight(1f))
        Text(text = "›", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.section)
    }
}

/**
 * 멤버 섹션: 인원/정원 + 멤버 목록(닉네임·역할 뱃지·매너온도) + 하단 탈퇴/삭제 액션.
 * 디자인 시안 부재 — 방 홈 섹션 카드 컨벤션을 따른다.
 *
 * 탈퇴는 비방장만, 삭제는 방장 + 참여자(방장 제외) 0명일 때만 노출한다(서버 규칙과 동일).
 * 방장은 각 멤버 행의 관리 메뉴로 공동 관리자 임명/해제·방장 위임을 수행한다.
 * 실제 실행은 확인 다이얼로그를 거쳐 콜백으로 올려보낸다.
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
                // 방장은 방장 자신(OWNER)을 제외한 멤버를 관리한다.
                ownerManage = myRole.isOwner && !member.role.isOwner,
                // 관리자는 본인 행에서만 스스로 관리자 해제(self-DEMOTE)할 수 있다.
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

/**
 * 멤버 관리 메뉴("⋯"). [selfDemote] 면 관리자 본인의 "관리자 그만두기"만,
 * 아니면 방장이 대상 역할에 따라 임명/해제·방장 위임 항목을 노출한다.
 */
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

/** 대기 중인 방장 위임 요청 배너 + 취소. */
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

/** 방장/관리자만 뱃지를 노출하고, 일반 멤버는 뱃지를 생략한다. */
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
