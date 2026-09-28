package com.ruleup.onboarding.presentation.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.AgreementType
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.ui.helper.LocalNavigationHelper

/** 약관 원문 열람. */
@Composable
fun TermsDocumentScreen(
    type: AgreementType,
    modifier: Modifier = Modifier,
) {
    TtiScreenEffect()
    val nav = LocalNavigationHelper.current
    TermsDocumentContent(type = type, onBack = { nav.navigateToBack() }, modifier = modifier)
}

@Composable
internal fun TermsDocumentContent(
    type: AgreementType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 파일이 없거나 읽히지 않아도 화면이 죽지 않는다
    val blocks =
        remember(type) {
            type
                .assetName()
                ?.let { name ->
                    runCatching {
                        context.assets
                            .open("terms/$name")
                            .bufferedReader()
                            .use { it.readText() }
                    }.getOrNull()
                }?.let(::parseTermsMarkdown)
                .orEmpty()
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        RuleUpTopBar(title = type.documentTitle(), onBack = onBack)
        if (blocks.isEmpty()) {
            Text(
                text = "약관을 불러오지 못했어요. 고객센터로 문의해 주세요.",
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.body,
                modifier = Modifier.padding(20.dp),
            )
            return@Column
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            blocks.forEach { TermsBlockRow(it) }
        }
    }
}

@Composable
private fun TermsBlockRow(block: TermsBlock) {
    when (block) {
        is TermsBlock.Heading ->
            Text(
                text = block.text,
                color = RuleUpTheme.colors.textPrimary,
                style =
                    when (block.level) {
                        1 -> RuleUpTheme.typography.title
                        2 -> RuleUpTheme.typography.section
                        else -> RuleUpTheme.typography.cardTitle
                    },
                modifier = Modifier.padding(top = 12.dp),
            )

        is TermsBlock.Paragraph ->
            Text(
                text = block.text,
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.body,
            )

        is TermsBlock.Bullet ->
            Row(
                modifier = Modifier.padding(start = (block.depth * 12).dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "·", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.body)
                Text(text = block.text, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.body)
            }

        // 표는 셀을 줄로 폈다
        is TermsBlock.TableRow ->
            Text(
                text = block.cells.joinToString(" · "),
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.small,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(RuleUpTheme.colors.surfaceVariant, RuleUpTheme.shapes.medium)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
            )

        TermsBlock.Divider ->
            HorizontalDivider(
                color = RuleUpTheme.colors.border,
                modifier = Modifier.padding(vertical = 4.dp),
            )
    }
}

/** 번들된 원문 파일. */
internal fun AgreementType.assetName(): String? =
    when (this) {
        AgreementType.TERMS_OF_SERVICE -> "terms_of_service.md"
        AgreementType.PRIVACY_POLICY -> "privacy_policy.md"
        AgreementType.LOCATION_SERVICE -> "location_service.md"
        else -> null
    }

/** 화면 제목. */
internal fun AgreementType.documentTitle(): String =
    when (this) {
        AgreementType.TERMS_OF_SERVICE -> "서비스 이용약관"
        AgreementType.PRIVACY_POLICY -> "개인정보 처리방침"
        AgreementType.LOCATION_SERVICE -> "위치기반서비스 이용약관"
        else -> "약관"
    }

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TermsDocumentScreenPreview() {
    com.ruleup.ui.helper.PreviewEnvironment {
        RuleUpTheme {
            TermsDocumentScreen(
                type =
                    com.ruleup.domain.entity.user.AgreementType.entries
                        .first(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun TermsDocumentContentPreview() {
    RuleUpTheme {
        TermsDocumentContent(
            type =
                com.ruleup.domain.entity.user.AgreementType.entries
                    .first(),
            onBack = { },
        )
    }
}
