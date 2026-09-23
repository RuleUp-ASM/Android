package com.ruleup.report.presentation.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.ruleUpCardSurface
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.report.domain.entity.HiddenEffect
import com.ruleup.report.domain.entity.ReportReason
import com.ruleup.report.presentation.report.viewmodel.ReportIntent
import com.ruleup.report.presentation.report.viewmodel.ReportState
import com.ruleup.report.presentation.report.viewmodel.ReportViewModel
import com.ruleup.ui.helper.LocalMessageHelper

/**
 * 신고하기 (Figma `1466:96`).
 *
 * **자유 텍스트 입력칸을 두지 않는다.** 디자인에는 "자세한 내용 (선택)" 칸이 있지만 신고 접수
 * 명세가 2026-08-26 개편에서 `detail` 을 폐기하고 클라이언트에서 입력란을 제거하라고 정했다.
 * 서버가 받지 않는 칸을 두면 사용자는 쓴 글이 검토에 쓰인다고 믿는다.
 */
@Composable
fun ReportScreen(
    targetName: String,
    userId: String? = null,
    challengeId: String? = null,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(userId, challengeId) {
        viewModel.onIntent(ReportIntent.Init(userId = userId, challengeId = challengeId, targetName = targetName))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is com.ruleup.report.presentation.report.viewmodel.ReportEffect.ShowMessage ->
                    messageHelper.showToast(effect.message)
            }
        }
    }
    // 접수되면 대상이 내 화면에서 가려진다 — 결과를 알리고 곧바로 돌아간다.
    LaunchedEffect(state.done) {
        val effect = state.done ?: return@LaunchedEffect
        messageHelper.showToast(effect.doneMessage())
        viewModel.onDoneDismissed()
    }
    ReportContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
internal fun ReportContent(
    state: ReportState,
    onIntent: (ReportIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        ReportAppBar(onBack = { onIntent(ReportIntent.Back) })
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = RuleUpTheme.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
        ) {
            TargetCard(state.targetName)
            Text(
                text = "어떤 문제가 있나요?",
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.section,
            )
            ReasonList(
                reasons = state.reasons,
                selected = state.selected,
                onSelect = { onIntent(ReportIntent.SelectReason(it)) },
            )
            InfoCard()
        }
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = RuleUpTheme.spacing.md, bottom = 28.dp),
        ) {
            RuleUpPrimaryButton(
                text = if (state.isSubmitting) "접수 중" else "신고하기",
                enabled = state.canSubmit,
                onClick = { onIntent(ReportIntent.Submit) },
            )
        }
    }
}

@Composable
private fun ReportAppBar(onBack: () -> Unit) {
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
        Text("신고하기", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.section)
        Box(Modifier.size(36.dp))
    }
}

/** 무엇을 신고하는지 다시 보여 준다 — 목록에서 눌러 들어오면 대상을 착각하기 쉽다. */
@Composable
private fun TargetCard(targetName: String) {
    Row(
        modifier = Modifier.ruleUpCardSurface(PaddingValues(horizontal = 14.dp, vertical = RuleUpTheme.spacing.md)),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RuleUpTheme.shapes.pill)
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = targetName.take(1),
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.bodyBold,
            )
        }
        Text(
            text = targetName,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ReasonList(
    reasons: List<ReportReason>,
    selected: ReportReason?,
    onSelect: (ReportReason) -> Unit,
) {
    Column(modifier = Modifier.ruleUpCardSurface(PaddingValues(vertical = RuleUpTheme.spacing.xxs))) {
        reasons.forEach { reason ->
            ReasonRow(
                label = reason.label(),
                selected = reason == selected,
                onClick = { onSelect(reason) },
            )
        }
    }
}

@Composable
private fun ReasonRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .singleClickable(onClick = onClick)
                .padding(horizontal = RuleUpTheme.spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioDot(selected)
        Text(
            text = label,
            color = RuleUpTheme.colors.textPrimary,
            style = if (selected) RuleUpTheme.typography.bodyBold else RuleUpTheme.typography.bodyMedium,
        )
    }
}

/** 색만으로 구분하지 않는다 — 고른 항목은 글자 굵기도 함께 바뀐다. */
@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        modifier =
            Modifier
                .size(20.dp)
                .clip(RuleUpTheme.shapes.pill)
                .then(
                    if (selected) {
                        Modifier.background(RuleUpTheme.colors.brand)
                    } else {
                        Modifier.border(1.5.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.pill)
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(RuleUpTheme.shapes.pill)
                        .background(RuleUpTheme.colors.surface),
            )
        }
    }
}

/** 접수 뒤에 무슨 일이 일어나는지 미리 말한다 — 모르면 "무시당했다"고 읽는다. */
@Composable
private fun InfoCard() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.brandSoft)
                .padding(horizontal = 14.dp, vertical = RuleUpTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xxs),
    ) {
        listOf(
            "· 신고는 익명으로 처리돼요",
            "· 신고하면 이 사람의 글과 프로필이 내 화면에서 바로 가려져요",
            "· 처리 결과는 따로 알려드리지 않아요",
        ).forEach {
            Text(text = it, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.small)
        }
    }
}

/** 서버가 알려 준 즉시 효과를 그대로 말한다 — 무엇이 가려졌는지 모르면 먹혔는지 알 수 없다. */
private fun HiddenEffect.doneMessage(): String =
    when (this) {
        HiddenEffect.USER_CONTENT_MASKED -> "신고했어요. 이 사람의 글과 프로필이 내 화면에서 가려져요."
        HiddenEffect.CHALLENGE_HIDDEN -> "신고했어요. 이 챌린지는 탐색에서 보이지 않아요."
        HiddenEffect.CHALLENGE_MASKED -> "신고했어요. 참여 중인 챌린지라 이름과 이미지만 가렸어요."
    }

@Preview
@Composable
private fun ReportPreview() {
    RuleUpTheme {
        ReportContent(
            state = ReportState.initial.copy(targetName = "지현", reasons = ReportReason.forUser),
            onIntent = {},
        )
    }
}
