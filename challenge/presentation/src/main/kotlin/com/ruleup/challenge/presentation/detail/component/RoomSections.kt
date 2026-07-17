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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.domain.entity.NoticeSummary
import com.ruleup.challenge.domain.entity.RankingEntry
import com.ruleup.challenge.domain.entity.RoomSummary
import com.ruleup.challenge.domain.entity.TodayVerificationStatus
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpPalette
import com.ruleup.ui.theme.RuleUpTheme
import java.util.Locale

// 방 홈 섹션 모음 (그룹 챌린지 ACTIVE 멤버 전용 — GET room 성공 시 상세에 확장 렌더링).
// 요약 3카드 스타일은 피그마 "챌린지 상세 그룹"(420:321) 시안, 공지·랭킹 섹션은 시안 부재로
// 기존 디자인 시스템 토큰으로 구성했다.

/** 완주율 · 평균 매너 · 남은 기간 3카드 (시안 420:347~355). */
@Composable
internal fun RoomSummaryRow(summary: RoomSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RoomStatCard(
            value = "${summary.completionRate.trimPercent()}%",
            label = "완주율",
            valueColor = RuleUpTheme.colors.brand,
            modifier = Modifier.weight(1f),
        )
        RoomStatCard(
            value = "${summary.avgMannerTemperature.trimPercent()}℃",
            label = "평균 매너",
            valueColor = RuleUpPalette.Amber500,
            modifier = Modifier.weight(1f),
        )
        RoomStatCard(
            value = "${summary.remainingDays}일",
            label = "남은 기간",
            valueColor = RuleUpTheme.colors.success,
            modifier = Modifier.weight(1f),
        )
    }
}

// 92.0 → "92", 92.5 → "92.5" (시안은 정수 표기 — 소수점이 있으면 그대로 노출)
private fun Double.trimPercent(): String =
    if (this % 1.0 == 0.0) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
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
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 공지 섹션: 고정 공지 배너(있으면) + 목록 진입 행(미읽음 뱃지).
 * 상세 조회 = 읽음 처리라 별도 읽음 버튼은 없다.
 */
@Composable
internal fun RoomNoticeSection(
    pinnedNotice: NoticeSummary?,
    unreadCount: Int,
    onOpenNotices: () -> Unit,
    onOpenNotice: (String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .singleClickable(onClick = onOpenNotices),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("공지")
            if (unreadCount > 0) {
                Spacer(Modifier.width(6.dp))
                UnreadBadge(unreadCount)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "전체 보기 ›",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
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
                Text(text = "📌", fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pinnedNotice.title,
                    color = RuleUpTheme.colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!pinnedNotice.isRead) {
                    Spacer(Modifier.width(8.dp))
                    Dot(RuleUpTheme.colors.danger)
                }
            }
        } else {
            Text(
                text = "등록된 고정 공지가 없어요",
                color = RuleUpTheme.colors.textMuted,
                fontSize = 12.sp,
            )
        }
    }
}

/** 그룹 랭킹 섹션: top3 미리보기 + 전체 랭킹 진입 (시안 부재 — 랭킹 화면 시안 434:514 의 행 구성을 축약). */
@Composable
internal fun RoomRankingSection(
    topRanking: List<RankingEntry>,
    onOpenRanking: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        if (topRanking.isEmpty()) {
            Text(
                text = "아직 랭킹이 집계되지 않았어요",
                color = RuleUpTheme.colors.textMuted,
                fontSize = 12.sp,
            )
        } else {
            topRanking.forEach { entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = rankEmoji(entry.rank),
                        fontSize = 14.sp,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = entry.nickname,
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${entry.progressRate.trimPercent()}%",
                        color = RuleUpTheme.colors.brand,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
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

/** 내 오늘 상태 (myTodayStatus — ChallengeMember.todayStatus 비정규화 값). */
@Composable
internal fun RoomTodayStatusCard(status: TodayVerificationStatus) {
    val (label, color) =
        when (status) {
            TodayVerificationStatus.SUCCESS -> "오늘 인증 완료" to RuleUpTheme.colors.success
            TodayVerificationStatus.PENDING -> "오늘 인증 대기 중" to RuleUpPalette.Amber500
            TodayVerificationStatus.FAILED -> "오늘 인증 실패" to RuleUpTheme.colors.danger
            TodayVerificationStatus.NOT_TARGET,
            TodayVerificationStatus.NOT_REQUIRED,
            -> "오늘은 인증 대상일이 아니에요" to RuleUpTheme.colors.textMuted
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle("오늘 내 상태")
        Spacer(Modifier.weight(1f))
        Dot(color)
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
internal fun UnreadBadge(count: Int) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(RuleUpTheme.colors.danger)
                .padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else "$count",
            color = RuleUpTheme.colors.surface,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
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
