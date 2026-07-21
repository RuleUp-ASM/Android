package com.ruleup.verification.presentation.pending

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.ui.helper.LocalMessageHelper
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme
import com.ruleup.verification.domain.entity.ObjectionDecision
import com.ruleup.verification.domain.entity.PendingReviewItem
import com.ruleup.verification.domain.entity.ReviewKind
import com.ruleup.verification.presentation.pending.viewmodel.PendingReviewsEffect
import com.ruleup.verification.presentation.pending.viewmodel.PendingReviewsIntent
import com.ruleup.verification.presentation.pending.viewmodel.PendingReviewsState
import com.ruleup.verification.presentation.pending.viewmodel.PendingReviewsViewModel

/**
 * 방장/공동 관리자 확인 대기함(명세 pending-reviews). 폴백 수동 인증·이의 제기를 통합 표시하고,
 * 이의 제기 항목은 승인/기각할 수 있다. 디자인 시안 부재 — 방 홈 섹션 카드 컨벤션을 따른다.
 */
@Composable
fun PendingReviewsScreen(
    challengeId: String,
    modifier: Modifier = Modifier,
    viewModel: PendingReviewsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current

    LaunchedEffect(challengeId) { viewModel.onIntent(PendingReviewsIntent.Load(challengeId)) }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PendingReviewsEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    PendingReviewsContent(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
private fun PendingReviewsContent(
    state: PendingReviewsState,
    onIntent: (PendingReviewsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 승인/기각 확인 다이얼로그 대상(objectionId to decision).
    var confirm by remember { mutableStateOf<Pair<String, ObjectionDecision>?>(null) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        TopBar(onBack = { onIntent(PendingReviewsIntent.Back) })

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.error != null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.error, color = RuleUpTheme.colors.textSecondary, fontSize = 14.sp)
                }

            state.reviews?.items.isNullOrEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("확인할 항목이 없어요", color = RuleUpTheme.colors.textMuted, fontSize = 14.sp)
                }

            else ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.reviews.items.forEach { item ->
                        ReviewCard(
                            item = item,
                            actionEnabled = !state.isDeciding,
                            onApprove = { confirm = item.id to ObjectionDecision.APPROVE },
                            onReject = { confirm = item.id to ObjectionDecision.REJECT },
                        )
                    }
                }
        }
    }

    confirm?.let { (objectionId, decision) ->
        val approve = decision == ObjectionDecision.APPROVE
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = RuleUpTheme.colors.surface,
            title = {
                Text(
                    if (approve) "이의 제기를 승인할까요?" else "이의 제기를 기각할까요?",
                    color = RuleUpTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    if (approve) "승인하면 해당 일자가 성공으로 확정돼요." else "기각하면 실패가 유지돼요.",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onIntent(PendingReviewsIntent.Decide(objectionId, decision))
                }) {
                    Text(
                        if (approve) "승인" else "기각",
                        color = if (approve) RuleUpTheme.colors.brand else RuleUpTheme.colors.danger,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text("취소", color = RuleUpTheme.colors.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = RuleUpTheme.colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
        Text("확인 대기함", color = RuleUpTheme.colors.textPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReviewCard(
    item: PendingReviewItem,
    actionEnabled: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KindBadge(item.kind)
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.nickname,
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(item.targetDate, color = RuleUpTheme.colors.textSecondary, fontSize = 12.sp)
        }

        item.content?.takeIf { it.isNotBlank() }?.let {
            Text(it, color = RuleUpTheme.colors.textSlate, fontSize = 13.sp)
        }

        item.deadline?.let {
            Text("마감 $it", color = RuleUpTheme.colors.textMuted, fontSize = 11.sp)
        }

        // 이의 제기만 승인/기각 가능. 폴백 수동 인증은 결정 API 부재로 읽기 전용.
        if (item.kind == ReviewKind.OBJECTION) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DecisionButton(
                    text = "기각",
                    color = RuleUpTheme.colors.danger,
                    enabled = actionEnabled,
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                )
                DecisionButton(
                    text = "승인",
                    color = RuleUpTheme.colors.brand,
                    enabled = actionEnabled,
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun KindBadge(kind: ReviewKind) {
    val (label, color) =
        when (kind) {
            ReviewKind.OBJECTION -> "이의 제기" to RuleUpTheme.colors.brand
            ReviewKind.FALLBACK -> "폴백 인증" to RuleUpTheme.colors.textSlate
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DecisionButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val effective = if (enabled) color else RuleUpTheme.colors.textMuted
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, effective, RoundedCornerShape(12.dp))
                .then(if (enabled) Modifier.singleClickable(onClick = onClick) else Modifier)
                .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = effective, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
