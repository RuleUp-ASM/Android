package com.ruleup.profile.presentation.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.designsystem.component.RuleUpNetworkError
import com.ruleup.designsystem.component.RuleUpSuspendedSheet
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.Tier
import com.ruleup.profile.domain.entity.MemberProfile
import com.ruleup.profile.presentation.common.accentColor
import com.ruleup.profile.presentation.common.label
import com.ruleup.profile.presentation.member.viewmodel.MemberProfileIntent
import com.ruleup.profile.presentation.member.viewmodel.MemberProfileState
import com.ruleup.profile.presentation.member.viewmodel.MemberProfileViewModel

/**
 * 타인 프로필 (Figma `1466:2` · 차단 변형 `1466:44`).
 *
 * 화면이 짧은 것이 계약이다 — 공개되는 값이 넷뿐이라 더 그릴 게 없다. 하단 한 줄이 그 사실을
 * 말해 주지 않으면 사용자는 로딩이 덜 됐다고 읽는다.
 */
@Composable
fun MemberProfileScreen(viewModel: MemberProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(MemberProfileIntent.Load) }
    MemberProfileContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun MemberProfileContent(
    state: MemberProfileState,
    onIntent: (MemberProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        // 정책이 진입점을 숨기지 말라고 정했다 — 눌렀을 때 왜 막혔는지 말한다(Figma 1465:141).
        state.reportBlock?.let { block ->
            RuleUpSuspendedSheet(
                title = "지금은 신고할 수 없어요",
                description = "허위 신고 반복으로 신고 기능이 정지됐어요.",
                until = block.until,
                onConfirm = { onIntent(MemberProfileIntent.DismissReportBlock) },
                onOpenHistory = { onIntent(MemberProfileIntent.OpenSanctionHistory) },
                onDismiss = { onIntent(MemberProfileIntent.DismissReportBlock) },
            )
        }
        MemberProfileAppBar(
            // 신고는 대상을 받아야 열 수 있다 — 아직 못 받았거나 차단해 둔 상대는 진입점을 숨긴다.
            onReport = { onIntent(MemberProfileIntent.Report) }.takeIf { state.profile?.blocked == false },
            onBack = { onIntent(MemberProfileIntent.Back) },
        )
        when {
            state.isOffline -> RuleUpNetworkError(onRetry = { onIntent(MemberProfileIntent.Retry) })

            state.isLoading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.profile == null ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "프로필을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.body,
                    )
                }

            else ->
                MemberProfileBody(
                    profile = state.profile,
                    isUnblocking = state.isUnblocking,
                    onUnblock = { onIntent(MemberProfileIntent.Unblock) },
                )
        }
    }
}

@Composable
private fun MemberProfileAppBar(
    onReport: (() -> Unit)?,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = RuleUpTheme.spacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text("프로필", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.section)
        if (onReport != null) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RuleUpTheme.shapes.pill)
                        .background(RuleUpTheme.colors.surface)
                        .singleClickable(onClick = onReport),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(com.ruleup.designsystem.R.drawable.ic_info),
                    contentDescription = "신고하기",
                    tint = RuleUpTheme.colors.danger,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Box(Modifier.size(36.dp))
        }
    }
}

@Composable
private fun MemberProfileBody(
    profile: MemberProfile,
    isUnblocking: Boolean,
    onUnblock: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = RuleUpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
    ) {
        ProfileCard(profile)
        CompletedCard(profile.completedChallengeCount)
        if (profile.blocked) BlockedCard(isUnblocking = isUnblocking, onUnblock = onUnblock)
        PrivacyNote()
    }
}

@Composable
private fun ProfileCard(profile: MemberProfile) {
    Column(
        modifier =
            Modifier.ruleUpCardSurface(
                androidx.compose.foundation.layout.PaddingValues(
                    start = RuleUpTheme.spacing.lg,
                    end = RuleUpTheme.spacing.lg,
                    top = RuleUpTheme.spacing.xxl,
                    bottom = RuleUpTheme.spacing.xl,
                ),
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(80.dp)
                    .clip(RuleUpTheme.shapes.pill)
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (profile.profileImageUrl != null) {
                AsyncImage(
                    model = profile.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RuleUpTheme.shapes.pill),
                )
            } else {
                // 사진이 없으면 닉네임 첫 글자. 탈퇴·차단으로 이름이 대체돼도 규칙은 같다.
                Text(
                    text = profile.nickname.take(1),
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.numberL,
                )
            }
        }
        Text(
            text = if (profile.withdrawn) "탈퇴한 사용자" else profile.nickname,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
        )
        // 탈퇴한 사용자의 티어는 남은 값일 뿐 지금을 말하지 않는다 — 그리지 않는다.
        if (!profile.withdrawn) TierChip(profile.tier)
    }
}

@Composable
private fun TierChip(tier: Tier) {
    Row(
        modifier =
            Modifier
                .clip(RuleUpTheme.shapes.pill)
                .background(tier.accentColor.copy(alpha = TIER_CHIP_BACKGROUND_ALPHA))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = tier.label,
            color = tier.accentColor,
            style = RuleUpTheme.typography.captionBold,
        )
    }
}

@Composable
private fun CompletedCard(count: Int) {
    Row(
        modifier = Modifier.ruleUpCardSurface(),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RuleUpTheme.shapes.pill)
                    .background(RuleUpTheme.colors.successContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_check),
                contentDescription = null,
                tint = RuleUpTheme.colors.success,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = "완주한 챌린지",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text("${count}개", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.numberM)
    }
}

/** 차단해 둔 상대. 이름·사진이 왜 다른지 말해 주고 해제 경로를 준다(Figma `1466:44`). */
@Composable
private fun BlockedCard(
    isUnblocking: Boolean,
    onUnblock: () -> Unit,
) {
    Column(
        modifier = Modifier.ruleUpCardSurface(),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        Text("내가 차단한 사용자예요", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
        Text(
            text = "차단하면 닉네임과 사진이 내 화면에서 가려져요. 차단은 상대에게 알리지 않아요.",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RuleUpTheme.shapes.medium)
                    .background(RuleUpTheme.colors.surfaceVariant)
                    .singleClickable(enabled = !isUnblocking, onClick = onUnblock),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isUnblocking) "해제하는 중" else "차단 해제",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
    }
}

/** 화면이 짧은 이유. 이 줄이 없으면 나머지가 로딩 중이라고 읽힌다. */
@Composable
private fun PrivacyNote() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = RuleUpTheme.spacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_lock),
            contentDescription = null,
            tint = RuleUpTheme.colors.textMuted,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = "진행 중인 챌린지·통계·티어 점수는 공개되지 않아요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.tiny,
            textAlign = TextAlign.Center,
        )
    }
}

/** 티어 칩 배경. Figma 는 티어마다 옅은 배경을 쓰는데 팔레트에 그 단계가 없어 강조색을 낮춰 쓴다. */
private const val TIER_CHIP_BACKGROUND_ALPHA = 0.12f

@Preview
@Composable
private fun MemberProfilePreview() {
    RuleUpTheme {
        MemberProfileContent(
            state =
                MemberProfileState.initial.copy(
                    isLoading = false,
                    profile =
                        MemberProfile(
                            userId = "u1",
                            nickname = "지현",
                            profileImageUrl = null,
                            tier = Tier.GOLD,
                            completedChallengeCount = 12,
                            withdrawn = false,
                            blocked = false,
                        ),
                ),
            onIntent = {},
        )
    }
}
