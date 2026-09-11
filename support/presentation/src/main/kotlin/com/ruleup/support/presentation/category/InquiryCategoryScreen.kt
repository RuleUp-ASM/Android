package com.ruleup.support.presentation.category

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.support.domain.entity.InquiryCategory
import com.ruleup.support.presentation.category.viewmodel.InquiryCategoryIntent
import com.ruleup.support.presentation.category.viewmodel.InquiryCategoryViewModel
import com.ruleup.support.presentation.category.viewmodel.InquiryShortcut

/**
 * 문의하기 · 카테고리 (Figma `1417:2`).
 *
 * 분류를 먼저 받는 이유는 담당자 배정이 여기서 갈리기 때문이다 — 본문만 받으면 운영자가 읽고
 * 다시 나눠야 하고 그만큼 답변이 늦어진다.
 *
 * Figma 와 다르게 간 곳
 * - **「유저·챌린지를 신고할래요」 바로가기를 빼고 안내 문장으로 바꿨다.** 신고는 대상이 있어야
 *   성립해 전용 진입 화면이 없다. 누를 곳이 없는 칩을 두면 눌러 보고 아무 일도 안 일어난다.
 */
@Composable
fun InquiryCategoryScreen(
    modifier: Modifier = Modifier,
    viewModel: InquiryCategoryViewModel = hiltViewModel(),
) {
    InquiryCategoryContent(onIntent = viewModel::onIntent, modifier = modifier)
}

@Composable
internal fun InquiryCategoryContent(
    onIntent: (InquiryCategoryIntent) -> Unit,
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
        RuleUpTopBar(title = "문의하기", onBack = { onIntent(InquiryCategoryIntent.Back) })

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
        ) {
            GuideBanner()
            Spacer(Modifier.height(18.dp))
            Text(
                text = "무엇을 도와드릴까요?",
                color = colors.textPrimary,
                style = RuleUpTheme.typography.section,
            )
            Spacer(Modifier.height(10.dp))
            CategoryList(onSelect = { onIntent(InquiryCategoryIntent.Select(it)) })
            Spacer(Modifier.height(12.dp))
            Text(
                text = OPERATION_NOTICE,
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
            Spacer(Modifier.height(16.dp))
            ShortcutCard(onOpen = { onIntent(InquiryCategoryIntent.OpenShortcut(it)) })
            Spacer(Modifier.height(24.dp))
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp).navigationBarsPadding()) {
            RuleUpPrimaryButton(
                text = "내 문의 내역",
                onClick = { onIntent(InquiryCategoryIntent.OpenHistory) },
            )
        }
    }
}

@Composable
private fun GuideBanner() {
    val colors = RuleUpTheme.colors
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.brandSoft)
                .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = "어떤 문제인지 골라 주세요 — 분류에 따라 담당자에게 바로 전달돼요",
            color = colors.textSecondary,
            style = RuleUpTheme.typography.small,
        )
    }
}

@Composable
private fun CategoryList(onSelect: (InquiryCategory) -> Unit) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface),
    ) {
        InquiryCategory.entries.forEachIndexed { index, category ->
            if (index > 0) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            }
            CategoryRow(category = category, onClick = { onSelect(category) })
        }
    }
}

@Composable
private fun CategoryRow(
    category: InquiryCategory,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .singleClickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.title,
                color = colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = category.description,
                color = colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Text(text = "›", color = colors.textMuted, style = RuleUpTheme.typography.body)
    }
}

@Composable
private fun ShortcutCard(onOpen: (InquiryShortcut) -> Unit) {
    val colors = RuleUpTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(colors.surface)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "이런 건 더 빠른 길이 있어요",
            color = colors.textPrimary,
            style = RuleUpTheme.typography.cardTitle,
        )
        InquiryShortcut.entries.forEach { shortcut ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = shortcut.question,
                    color = colors.textSecondary,
                    style = RuleUpTheme.typography.small,
                    modifier = Modifier.weight(1f),
                )
                ShortcutChip(label = shortcut.actionLabel, onClick = { onOpen(shortcut) })
            }
        }
        Text(
            text = "유저 · 챌린지 신고는 신고할 대상의 프로필이나 방에서 할 수 있어요",
            color = colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

@Composable
private fun ShortcutChip(
    label: String,
    onClick: () -> Unit,
) {
    val colors = RuleUpTheme.colors
    Box(
        modifier =
            Modifier
                .clip(RuleUpTheme.shapes.small)
                .background(colors.brandSoft)
                .singleClickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(text = label, color = colors.brand, style = RuleUpTheme.typography.captionMedium)
    }
}

/** 운영 시간·응답 기한·하루 상한을 한 줄로 붙여 둔다 — 접수 전에 기대치를 맞추는 문장이다. */
private const val OPERATION_NOTICE =
    "평일 10:00~18:00 운영 · 영업일 2일 안에 답변드려요 · 하루 3건까지 접수할 수 있어요"
