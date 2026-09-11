package com.ruleup.support.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.component.StatusChip
import com.ruleup.designsystem.component.StatusChipTone
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.support.domain.entity.InquiryDetail
import com.ruleup.support.domain.entity.InquiryStatus
import com.ruleup.support.presentation.common.shortDateTime
import com.ruleup.support.presentation.common.shortInquiryId
import com.ruleup.support.presentation.detail.viewmodel.InquiryDetailIntent
import com.ruleup.support.presentation.detail.viewmodel.InquiryDetailState
import com.ruleup.support.presentation.detail.viewmodel.InquiryDetailViewModel

/**
 * 문의 상세 (Figma `1419:87`).
 *
 * Figma 와 다르게 간 곳
 * - **"답변 후 7일 안에 한 번 더 질문할 수 있어요"를 뺐다.** 재문의가 명세에서 폐지됐다 — 같은
 *   사안이라도 새 문의로 받는다. 없는 경로를 안내하면 사용자가 입력창을 찾다 시간을 버린다.
 * - 그래서 하단이 입력바가 아니라 **새 문의 작성 진입**이다.
 */
@Composable
fun InquiryDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: InquiryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onIntent(InquiryDetailIntent.Load) }

    InquiryDetailContent(state = state, onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun InquiryDetailContent(
    state: InquiryDetailState,
    onIntent: (InquiryDetailIntent) -> Unit,
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
        RuleUpTopBar(title = "문의 상세", onBack = { onIntent(InquiryDetailIntent.Back) })

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.brand)
                    }

                state.detail == null ->
                    ErrorBody(
                        message = state.errorMessage ?: "문의를 불러오지 못했어요",
                        onRetry = { onIntent(InquiryDetailIntent.Retry) },
                    )

                else -> DetailBody(detail = state.detail)
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding()) {
            RuleUpPrimaryButton(
                text = "새 문의하기",
                onClick = { onIntent(InquiryDetailIntent.NewInquiry) },
            )
        }
    }
}

@Composable
private fun DetailBody(detail: InquiryDetail) {
    val colors = RuleUpTheme.colors
    val clipboard = LocalClipboardManager.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.medium)
                    .background(colors.surface)
                    .singleClickable { clipboard.setText(AnnotatedString(detail.inquiryId)) }
                    .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                detail.category?.let { category ->
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
                if (detail.status == InquiryStatus.ANSWERED) {
                    StatusChip(text = detail.status.label, tone = StatusChipTone.Success)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "접수번호 ${shortInquiryId(detail.inquiryId)} · ${shortDateTime(detail.createdAt)} 접수",
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(text = "내 문의", color = colors.textPrimary, style = RuleUpTheme.typography.section)
        Spacer(Modifier.height(8.dp))
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.medium)
                    .background(colors.surface)
                    .padding(16.dp),
        ) {
            Text(text = detail.body, color = colors.textPrimary, style = RuleUpTheme.typography.small)
            if (detail.imageUrls.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.imageUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "첨부한 사진",
                            contentScale = ContentScale.Crop,
                            modifier =
                                Modifier
                                    .size(52.dp)
                                    .clip(RuleUpTheme.shapes.small)
                                    .background(colors.surfaceVariant),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        AnswerSection(detail = detail)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AnswerSection(detail: InquiryDetail) {
    val colors = RuleUpTheme.colors
    val answer = detail.answerText

    if (answer == null) {
        // 미답변에 빈 자리를 두지 않는다 — 답변 영역이 비어 있으면 로딩 실패로 읽힌다.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.medium)
                    .background(colors.surfaceVariant)
                    .padding(16.dp),
        ) {
            Text(
                text = "아직 답변 전이에요. 영업일 2일 안에 답변드릴게요.",
                color = colors.textSecondary,
                style = RuleUpTheme.typography.small,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = "룰업 운영팀 답변", color = colors.textPrimary, style = RuleUpTheme.typography.section)
            Spacer(Modifier.size(8.dp))
            detail.answeredAt?.let {
                Text(
                    text = shortDateTime(it),
                    color = colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.medium)
                    .background(colors.brandSoft)
                    .padding(16.dp),
        ) {
            Text(text = answer, color = colors.textPrimary, style = RuleUpTheme.typography.small)
        }
    }
}

@Composable
private fun ErrorBody(
    message: String,
    onRetry: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            color = colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "다시 시도",
            color = colors.brand,
            style = RuleUpTheme.typography.labelMedium,
            modifier = Modifier.singleClickable(onClick = onRetry).padding(8.dp),
        )
    }
}
