package com.ruleup.challenge.presentation.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.domain.entity.RoutineTemplate
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.designsystem.R
import com.ruleup.designsystem.category.categoryIconRes
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper
import kotlinx.coroutines.delay

/**
 * 생성 입력 화면 (Figma `1134:544` · 생성 · 입력).
 *
 * 두 경로의 출발점이다. **경로 A(추천 칩)** 는 대기가 없고, **경로 B(설명 입력)** 만 LLM 로딩이 있다.
 * 둘 다 같은 확인 화면으로 수렴한다.
 *
 * 하단 고정 CTA 를 두지 않는다 — 디자인이 "만들기" 를 입력 박스 안에 넣었다. 입력과 실행이 붙어 있어야
 * 무엇에 대한 버튼인지 분명하고, 아래 추천 카드와도 섞이지 않는다.
 */
@Composable
fun ChallengeInputContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    state: CreateChallengeState = CreateChallengeState.initial,
) {
    val nav = LocalNavigationHelper.current

    // 초안 생성 중에는 화면을 잠그되 뒤로가기로 취소할 수 있게 한다(프론트 스펙 5).
    BackHandler(enabled = state.isDrafting) { onIntent(CreateChallengeIntent.CancelDrafting) }

    Box(modifier = modifier.fillMaxSize().background(RuleUpTheme.colors.background)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            InputAppBar(onClose = { nav.navigateToBack() })

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "어떤 습관을 만들까요?",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.numberM,
                )

                RoutineDescriptionBox(state = state, onIntent = onIntent)

                OrDivider()

                state.templates.forEach { template ->
                    TemplateCard(
                        template = template,
                        enabled = !state.isDrafting,
                        onClick = { onIntent(CreateChallengeIntent.SelectTemplate(template.templateId)) },
                    )
                }
                if (state.isLoadingTemplates) {
                    repeat(SKELETON_COUNT) { TemplateSkeleton() }
                }
                if (state.templatesFailed) {
                    RetryRow(onRetry = { onIntent(CreateChallengeIntent.RetryTemplates) })
                }

                Text(
                    text = "추천으로 시작하면 바로 초안이 만들어져요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }

        if (state.isDrafting) DraftingOverlay()
    }
}

@Composable
private fun InputAppBar(onClose: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "닫기",
            tint = RuleUpTheme.colors.textPrimary,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .size(22.dp)
                    .singleClickable(onClick = onClose),
        )
        Text(
            text = "새 챌린지",
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
    }
}

/**
 * 설명 입력 박스. 글자수와 "만들기" 가 **박스 안에** 있다.
 *
 * 429 로 막혀 있으면 버튼 자리에 남은 시간을 보여준다 — 언제 되는지 모르면 계속 누르게 된다.
 */
@Composable
private fun RoutineDescriptionBox(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.surface)
                .border(1.5.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.large)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(DESCRIPTION_MIN_HEIGHT)) {
            if (state.routineDescription.isEmpty()) {
                Text(
                    text = "평일 아침마다 헬스장에 가고 싶어요. 친구 3명이랑 같이 할 거예요",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.labelMedium,
                )
            }
            BasicTextField(
                value = state.routineDescription,
                onValueChange = { onIntent(CreateChallengeIntent.SetRoutineDescription(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isDrafting,
                textStyle = RuleUpTheme.typography.labelMedium.copy(color = RuleUpTheme.colors.textPrimary),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.routineDescription.length}/${CreateChallengeState.DESCRIPTION_MAX}",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.captionMedium,
            )
            SubmitButton(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun SubmitButton(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    val remaining = state.retryAfterSeconds
    val label =
        when {
            state.isDrafting -> "만드는 중…"
            remaining != null && remaining > 0 -> "${remaining}초 후"
            else -> "만들기"
        }
    val enabled = state.canSubmitDescription
    Box(
        modifier =
            Modifier
                .height(36.dp)
                .clip(RuleUpTheme.shapes.small)
                .background(if (enabled) RuleUpTheme.colors.brand else RuleUpTheme.colors.border)
                .singleClickable(enabled = enabled) { onIntent(CreateChallengeIntent.SubmitDescription) }
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = RuleUpTheme.colors.onSuccess, style = RuleUpTheme.typography.bodyBold)
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = RuleUpTheme.colors.border)
        Text("또는 바로 시작하기", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.captionMedium)
        HorizontalDivider(modifier = Modifier.weight(1f), color = RuleUpTheme.colors.border)
    }
}

/**
 * 추천 루틴 카드. 아이콘은 서버가 준 카테고리로 고른다 — 디자인의 3종 글리프를 그대로 박으면
 * 서버가 다른 루틴을 내려줄 때 아이콘이 어긋난다.
 */
@Composable
private fun TemplateCard(
    template: RoutineTemplate,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                .singleClickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.brandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(categoryIconRes(template.category)),
                contentDescription = null,
                tint = RuleUpTheme.colors.brand,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(template.title, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.cardTitle)
            // 추천 사유가 곧 부제다 — 디자인의 "원하는 시각 ±10분 · 주 5일" 자리.
            Text(template.reason, color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.captionMedium)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = RuleUpTheme.colors.textMuted,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TemplateSkeleton() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.surfaceVariant),
    )
}

@Composable
private fun RetryRow(onRetry: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(RuleUpTheme.colors.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("추천을 불러오지 못했어요", color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.bodyMedium)
        Text(
            "다시 시도",
            modifier = Modifier.singleClickable(onClick = onRetry),
            color = RuleUpTheme.colors.brand,
            style = RuleUpTheme.typography.smallMedium,
        )
    }
}

/**
 * 초안 생성 대기 오버레이.
 *
 * p95 5초 · 최대 10초까지 걸릴 수 있어 **문구를 단계적으로 바꿔** 멈춘 화면처럼 보이지 않게 한다.
 */
@Composable
private fun DraftingOverlay() {
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (stage < DRAFTING_MESSAGES.lastIndex) {
            delay(DRAFTING_STAGE_MS)
            stage += 1
        }
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background.copy(alpha = 0.92f))
                // 뒤 화면 조작을 막는다. 빠져나갈 길은 뒤로가기다.
                .singleClickable {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircularProgressIndicator(color = RuleUpTheme.colors.brand)
            Text(
                text = DRAFTING_MESSAGES[stage],
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.bodyMedium,
            )
            Text(
                text = "뒤로가기로 취소할 수 있어요",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

private val DRAFTING_MESSAGES =
    listOf(
        "루틴을 이해하는 중이에요",
        "어울리는 인증 방식을 고르는 중이에요",
        "거의 다 됐어요",
    )

private val DESCRIPTION_MIN_HEIGHT = 72.dp
private const val DRAFTING_STAGE_MS = 3_000L
private const val SKELETON_COUNT = 3
