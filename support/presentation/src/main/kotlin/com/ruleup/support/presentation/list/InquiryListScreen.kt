package com.ruleup.support.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.domain.entity.InquirySummary
import com.ruleup.support.presentation.common.shortDate
import com.ruleup.support.presentation.common.shortInquiryId
import com.ruleup.support.presentation.list.viewmodel.InquiryListIntent
import com.ruleup.support.presentation.list.viewmodel.InquiryListState
import com.ruleup.support.presentation.list.viewmodel.InquiryListViewModel

/**
 * 내 문의 내역 (Figma `1419:27`).
 *
 * 답변이 왔는지 확인하는 유일한 화면이다 — 답변을 알림함으로 알리지 않기로 해(2026-09-11)
 * 「새 답변」 점이 사용자가 답변을 알아채는 신호 전부다.
 */
@Composable
fun InquiryListScreen(
    modifier: Modifier = Modifier,
    viewModel: InquiryListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(InquiryListIntent.Load) }

    InquiryListContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun InquiryListContent(
    state: InquiryListState,
    onIntent: (InquiryListIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = "내 문의 내역", onBack = { onIntent(InquiryListIntent.Back) })

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.brand)
                    }

                state.errorMessage != null && state.isEmpty ->
                    MessageBody(
                        title = state.errorMessage,
                        description = "잠시 후 다시 시도해 주세요",
                        actionLabel = "다시 시도",
                        onAction = { onIntent(InquiryListIntent.Retry) },
                    )

                state.isEmpty ->
                    MessageBody(
                        title = "아직 접수한 문의가 없어요",
                        description = "문제가 생기면 언제든 문의해 주세요",
                    )

                else -> InquiryItems(state = state, onIntent = onIntent)
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding()) {
            RuleUpPrimaryButton(text = "문의하기", onClick = { onIntent(InquiryListIntent.NewInquiry) })
        }
    }
}

@Composable
private fun InquiryItems(
    state: InquiryListState,
    onIntent: (InquiryListIntent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(state.items, key = { it.inquiryId }) { item ->
            InquiryRow(item = item, onClick = { onIntent(InquiryListIntent.Open(item.inquiryId)) })
        }
    }
}

@Composable
private fun InquiryRow(
    item: InquirySummary,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface)
                .singleClickable(onClick = onClick)
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 모르는 분류는 칩만 비운다 — 본문과 접수 정보는 그대로 읽을 수 있어야 한다.
            item.category?.let { category ->
                Box(
                    modifier =
                        Modifier
                            .clip(RuleUpTheme.shapes.small)
                            .background(colors.brandSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = category.title,
                        color = colors.brand,
                        style = RuleUpTheme.typography.micro,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            if (item.status == InquiryStatus.ANSWERED) {
                StatusChip(text = item.status.label, tone = StatusChipTone.Success)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = item.preview,
            color = colors.textPrimary,
            style = RuleUpTheme.typography.bodyMedium,
        )

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${shortDate(item.createdAt)} 접수 · ${shortInquiryId(item.inquiryId)}",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
                modifier = Modifier.weight(1f),
            )
            if (item.hasNewAnswer) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(colors.danger))
                Spacer(Modifier.size(4.dp))
                Text(text = "새 답변", color = colors.danger, style = RuleUpTheme.typography.micro)
            }
        }
    }
}

@Composable
private fun MessageBody(
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            color = colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            color = colors.textMuted,
            style = RuleUpTheme.typography.small,
            textAlign = TextAlign.Center,
        )
        actionLabel?.let {
            Spacer(Modifier.height(16.dp))
            Text(
                text = it,
                color = colors.brand,
                style = RuleUpTheme.typography.labelMedium,
                modifier = Modifier.singleClickable(onClick = onAction).padding(8.dp),
            )
        }
    }
}
