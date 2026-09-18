package com.ruleup.profile.presentation.locked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.ruleup.designsystem.component.RuleUpNetworkError
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.profile.domain.entity.SanctionType
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.locked.viewmodel.AccountLockedIntent
import com.ruleup.profile.presentation.locked.viewmodel.AccountLockedState
import com.ruleup.profile.presentation.locked.viewmodel.AccountLockedViewModel

/**
 * 잠금 화면 (Figma `1465:37` 기간 · `1465:89` 영구).
 *
 * 두 디자인은 아이콘·제목·해제 예정 값·안내 문구만 다르고 뼈대가 같다. 화면을 둘로 나누면 같은
 * 레이아웃이 두 벌이 되고, 한쪽만 고치는 실수가 생긴다 — [ActiveSanction.type] 으로 가른다.
 *
 * **뒤로가기로 빠져나갈 수 없다.** 라우트를 루트로 등록해 백스택을 비운다 — 여기서 나갈 수 있으면
 * 게이트가 아니다.
 */
@Composable
fun AccountLockedScreen(viewModel: AccountLockedViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onIntent(AccountLockedIntent.Load) }
    AccountLockedContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun AccountLockedContent(
    state: AccountLockedState,
    onIntent: (AccountLockedIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isOffline) {
        RuleUpNetworkError(onRetry = { onIntent(AccountLockedIntent.Retry) }, modifier = modifier)
        return
    }
    val permanent = state.sanction?.type == SanctionType.BAN
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 36.dp),
            verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.lg),
        ) {
            LockedHero(permanent = permanent)
            state.sanction?.let { SanctionInfoCard(sanction = it, permanent = permanent) }
            NoticeCard(permanent = permanent)
            MenuCard(
                onOpenHistory = { onIntent(AccountLockedIntent.OpenHistory) },
                onOpenNotifications = { onIntent(AccountLockedIntent.OpenNotifications) },
            )
        }
        LockedFooter(
            // 재검토는 제재당 1회다. 서버가 이미 받았으면 버튼을 내려 보내지 않는다.
            reviewRequestable = state.sanction?.reviewRequestable ?: false,
            onRequestReview = { onIntent(AccountLockedIntent.RequestReview) },
            onLogout = { onIntent(AccountLockedIntent.Logout) },
        )
    }
}

@Composable
private fun LockedHero(permanent: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = RuleUpTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RuleUpTheme.shapes.pill)
                    .background(RuleUpTheme.colors.dangerContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter =
                    painterResource(
                        if (permanent) {
                            com.ruleup.profile.presentation.R.drawable.ic_sanction_ban
                        } else {
                            com.ruleup.designsystem.R.drawable.ic_lock
                        },
                    ),
                contentDescription = null,
                tint = RuleUpTheme.colors.danger,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = if (permanent) "계정이 영구 정지됐어요" else "계정 이용이 정지됐어요",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        Text(
            text =
                if (permanent) {
                    "운영 정책을 중대하게 위반해\n더 이상 앱을 이용할 수 없어요."
                } else {
                    "운영 정책 위반으로 한 달 동안\n앱을 이용할 수 없어요."
                },
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.body,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SanctionInfoCard(
    sanction: ActiveSanction,
    permanent: Boolean,
) {
    Column(
        modifier = Modifier.ruleUpCardSurface(),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
    ) {
        InfoRow(label = "제재 종류", value = if (permanent) "영구 정지" else "로그인 정지 · 1개월")
        // 모더레이션 거부 사유는 회피 방지로 상세가 오지 않는다 — 비면 줄을 지운다.
        sanction.reasonText?.let { InfoRow(label = "사유", value = it) }
        sanction.startsAt?.let { InfoRow(label = "정지 시작", value = dateDotLabel(it)) }
        InfoRow(
            label = "해제 예정",
            // 영구 정지에는 해제일이 없다. null 을 "곧 풀림"으로 접으면 안 된다.
            value = sanction.endsAt?.let(::dateDotLabel) ?: "해제되지 않아요",
            emphasize = true,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
    ) {
        Text(text = label, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.small)
        Text(
            text = value,
            color = if (emphasize) RuleUpTheme.colors.danger else RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.smallBold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun NoticeCard(permanent: Boolean) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.warningContainer)
                .padding(horizontal = 14.dp, vertical = RuleUpTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        Icon(
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_info),
            contentDescription = null,
            tint = RuleUpTheme.colors.warning,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text =
                if (permanent) {
                    "이 소셜 계정으로는 다시 가입할 수 없어요. 개인정보는 정책에 따라 2년 안에 파기돼요."
                } else {
                    "진행 중이던 챌린지는 자동으로 탈퇴됐어요. 알림함과 제재 이력은 계속 볼 수 있어요."
                },
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
    }
}

/** 잠금 중에도 열리는 두 화면. 정책이 허용한 것이 이 둘과 CS 문의뿐이다. */
@Composable
private fun MenuCard(
    onOpenHistory: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    Column(modifier = Modifier.ruleUpCardSurface(PaddingValues(0.dp))) {
        MenuRow(label = "제재 이력", onClick = onOpenHistory)
        MenuRow(label = "알림함", onClick = onOpenNotifications)
    }
}

@Composable
private fun MenuRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(46.dp)
                .singleClickable(onClick = onClick)
                .padding(horizontal = RuleUpTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RuleUpTheme.colors.textMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun LockedFooter(
    reviewRequestable: Boolean,
    onRequestReview: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = RuleUpTheme.spacing.md, bottom = RuleUpTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (reviewRequestable) {
            RuleUpPrimaryButton(text = "재검토 요청하기", onClick = onRequestReview)
            Text(
                text = "제재당 1회 · 영업일 5일 이내 회신 · 요청해도 정지는 유지돼요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.tiny,
                textAlign = TextAlign.Center,
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(36.dp).singleClickable(onClick = onLogout),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "로그아웃", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.smallMedium)
        }
    }
}

@Preview
@Composable
private fun AccountLockedPreview() {
    RuleUpTheme {
        AccountLockedContent(
            state =
                AccountLockedState.initial.copy(
                    isLoading = false,
                    sanction =
                        ActiveSanction(
                            sanctionId = "s1",
                            track = null,
                            type = SanctionType.LOCK,
                            featureCode = null,
                            reasonCode = "REPORT_REVIEW",
                            reasonText = "부정한 방법으로 인증 반복",
                            startsAt = "2026-09-15T00:00:00Z",
                            endsAt = "2026-10-15T00:00:00Z",
                            reviewRequestable = true,
                        ),
                ),
            onIntent = {},
        )
    }
}
