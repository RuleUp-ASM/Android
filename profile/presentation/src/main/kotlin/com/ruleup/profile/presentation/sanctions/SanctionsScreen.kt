package com.ruleup.profile.presentation.sanctions

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.FeatureCode
import com.ruleup.profile.domain.entity.ActiveSanction
import com.ruleup.profile.domain.entity.AdminSanction
import com.ruleup.profile.domain.entity.AutoSanction
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.domain.entity.SanctionType
import com.ruleup.profile.presentation.common.dateDotLabel
import com.ruleup.profile.presentation.sanctions.viewmodel.SanctionsIntent
import com.ruleup.profile.presentation.sanctions.viewmodel.SanctionsState
import com.ruleup.profile.presentation.sanctions.viewmodel.SanctionsViewModel
import com.ruleup.tti.presentation.TtiScreenEffect

/** 제재 통지·이력 (설정 허브 → 제재 이력). */
@Composable
fun SanctionsScreen(
    modifier: Modifier = Modifier,
    viewModel: SanctionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)

    LaunchedEffect(Unit) { viewModel.onIntent(SanctionsIntent.Load) }

    SanctionsContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

/** 화면 본문. */
@Composable
internal fun SanctionsContent(
    state: SanctionsState,
    onIntent: (SanctionsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "제재 이력", onBack = { onIntent(SanctionsIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.history == null ->
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "제재 이력을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { onIntent(SanctionsIntent.Load) }) { Text("다시 시도") }
                }

            state.history.isEmpty ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "받은 제재가 없어요",
                        color = RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                }

            else -> SanctionsBody(history = state.history)
        }
    }
}

@Composable
private fun SanctionsBody(history: SanctionHistory) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        history.activeSanction?.let { item { ActiveCard(sanction = it) } }

        if (history.admin.isNotEmpty()) {
            item { SectionLabel("운영자 제재") }
            items(history.admin) { AdminCard(sanction = it) }
        }
        if (history.auto.isNotEmpty()) {
            item { SectionLabel("자동 제재") }
            items(history.auto) { AutoCard(sanction = it) }
        }
        item {
            Text(
                text = "이 화면은 열람 전용이에요. 이의가 있으면 고객센터로 문의해 주세요.",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
            )
        }
    }
}

/** 지금 효력이 있는 제재. */
@Composable
private fun ActiveCard(sanction: ActiveSanction) {
    val endsAt = sanction.endsAt
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.dangerContainer)
                .padding(16.dp),
    ) {
        Text(
            text = sanction.type.label(sanction.featureCode),
            color = RuleUpTheme.colors.danger,
            style = RuleUpTheme.typography.cardTitle,
        )
        sanction.reasonText?.let {
            Spacer(Modifier.height(6.dp))
            Text(text = it, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.small)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text =
                if (sanction.type == SanctionType.BAN || endsAt == null) {
                    "해제일 없음 (영구)"
                } else {
                    "${dateDotLabel(endsAt)}까지"
                },
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.smallBold,
        )
        if (sanction.reviewRequestable) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "고객센터를 통해 재검토를 1회 요청할 수 있어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun AdminCard(sanction: AdminSanction) {
    HistoryCard(
        title = sanction.type.label(sanction.featureCode),
        subtitle = sanctionReasonLabel(sanction.reasonCode),
        trailing = sanction.startsAt?.let(::dateDotLabel).orEmpty(),
    )
}

@Composable
private fun AutoCard(sanction: AutoSanction) {
    val rejoinAt = sanction.rejoinAvailableAt
    HistoryCard(
        title = sanction.challengeTitle ?: sanction.type.label(),
        subtitle =
            when {
                sanction.permanent -> "다시 참여할 수 없어요"
                rejoinAt != null -> "${dateDotLabel(rejoinAt)}부터 재참여 가능"
                else -> sanction.reasonCode.orEmpty()
            },
        trailing = sanction.occurredAt?.let(::dateDotLabel).orEmpty(),
    )
}

@Composable
private fun HistoryCard(
    title: String,
    subtitle: String,
    trailing: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
            }
        }
        Text(text = trailing, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = RuleUpTheme.colors.textMuted,
        style = RuleUpTheme.typography.captionBold,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
    )
}

/** 제재 한 줄 제목. */
private fun SanctionType?.label(featureCode: String? = null): String =
    when (this) {
        SanctionType.FEATURE_SUSPENSION -> "${FeatureCode.label(featureCode)} 기능 정지"
        SanctionType.LOCK -> "계정 잠금"
        SanctionType.BAN -> "영구 정지"
        SanctionType.CHALLENGE_KICK -> "챌린지 강퇴"
        null -> "제재"
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun SanctionsContentPreview() {
    RuleUpTheme {
        SanctionsContent(
            state =
                com.ruleup.profile.presentation.sanctions.viewmodel.SanctionsState.initial
                    .copy(isLoading = false),
            onIntent = { },
        )
    }
}

internal fun sanctionReasonLabel(code: String?): String =
    when (code) {
        "REPORT_ABUSE" -> "신고 기능 남용"
        "REPORT_CONFIRMED" -> "신고 내용 확인에 따른 제재"
        else -> "운영 정책 위반"
    }
