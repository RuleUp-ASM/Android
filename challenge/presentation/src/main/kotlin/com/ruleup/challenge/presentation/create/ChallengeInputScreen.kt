package com.ruleup.challenge.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.presentation.create.component.CreateChallengeTopBar
import com.ruleup.challenge.presentation.create.component.SectionLabel
import com.ruleup.challenge.presentation.create.component.SmallBadge
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 생성 입력 화면 — 두 경로의 출발점.
 *
 * **경로 A(추천 칩)** 는 대기가 없고, **경로 B(설명 입력)** 만 LLM 로딩이 있다. 둘 다 같은 확인 화면으로
 * 수렴한다. 추천 조회가 실패해도 설명 입력은 계속 쓸 수 있어야 하므로 화면 전체를 에러로 만들지 않는다.
 */
@Composable
fun ChallengeInputContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    state: CreateChallengeState = CreateChallengeState.initial,
) {
    val nav = LocalNavigationHelper.current

    Column(modifier = modifier.fillMaxSize().background(RuleUpTheme.colors.background)) {
        CreateChallengeTopBar(title = "챌린지 만들기", onBack = { nav.navigateToBack() })

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            RecommendationSection(state = state, onIntent = onIntent)
            DescriptionSection(state = state, onIntent = onIntent)
            state.fallbackMessage?.let { FallbackBanner(message = it) }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            RuleUpPrimaryButton(
                text = submitLabel(state),
                enabled = state.canSubmitDescription,
                onClick = { onIntent(CreateChallengeIntent.SubmitDescription) },
            )
        }
    }
}

private fun submitLabel(state: CreateChallengeState): String =
    when {
        state.isDrafting -> "AI가 초안을 만드는 중…"
        state.retryAfterSeconds != null -> "잠시 후 다시 시도해 주세요"
        else -> "다음"
    }

/** 추천 루틴 3개. 서버가 항상 3개를 보장하므로 비어 있으면 실패했거나 로딩 중이다. */
@Composable
private fun RecommendationSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("지금 시작하기 좋은 루틴") {
            SmallBadge("바로 시작", RuleUpPalette.Primary50, RuleUpTheme.colors.brand)
        }
        when {
            state.isLoadingTemplates ->
                repeat(SKELETON_COUNT) { TemplateSkeleton() }

            state.templatesFailed ->
                RetryRow(onRetry = { onIntent(CreateChallengeIntent.RetryTemplates) })

            else ->
                state.templates.forEach { template ->
                    TemplateChip(
                        template = template,
                        enabled = !state.isDrafting,
                        onClick = { onIntent(CreateChallengeIntent.SelectTemplate(template.templateId)) },
                    )
                }
        }
    }
}

@Composable
private fun TemplateChip(
    template: RoutineTemplate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surface)
                .singleClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        template.category?.let { Text(categoryEmoji(it), style = RuleUpTheme.typography.body) }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(template.title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyMedium)
            Text(template.reason, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
        }
        Text("›", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.body)
    }
}

@Composable
private fun TemplateSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surfaceVariant),
    )
}

@Composable
private fun RetryRow(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "추천을 불러오지 못했어요",
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.bodyMedium,
        )
        Text(
            "다시 시도",
            modifier = Modifier.singleClickable(onClick = onRetry),
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallMedium,
        )
    }
}

@Composable
private fun DescriptionSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("직접 설명하기") {
            Text(
                "${state.routineDescription.length}/${CreateChallengeState.DESCRIPTION_MAX}",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surface)
                    .padding(14.dp),
        ) {
            if (state.routineDescription.isEmpty()) {
                Text(
                    "예) 매일 아침 6시에 일어나서 하루를 길게 쓰고 싶어요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.body,
                )
            }
            BasicTextField(
                value = state.routineDescription,
                onValueChange = { onIntent(CreateChallengeIntent.SetRoutineDescription(it)) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = RuleUpTheme.typography.body.copy(color = RuleUpTheme.colors.textPrimary),
                enabled = !state.isDrafting,
            )
        }
    }
}

/**
 * 폴백 안내. **에러 색을 쓰지 않는다** — 정상 분기인데 실패로 인지되면 그대로 이탈로 이어지기 때문이다.
 * 입력한 설명도 지우지 않는다.
 */
@Composable
private fun FallbackBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(RuleUpPalette.Primary50)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(message, color = RuleUpTheme.colors.textSlate, style = RuleUpTheme.typography.bodyMedium)
        Text(
            "위 추천 루틴에서 골라도 바로 시작할 수 있어요",
            color = RuleUpTheme.colors.textMuted,
            style = RuleUpTheme.typography.caption,
        )
    }
}

private const val SKELETON_COUNT = 3
