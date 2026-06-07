package com.ruleup.challenge.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.presentation.create.component.ChallengeFlowPreview
import com.ruleup.challenge.presentation.create.component.CreateChallengeTopBar
import com.ruleup.challenge.presentation.create.component.SmallBadge
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpPalette
import com.ruleup.ui.theme.RuleUpTheme

/** 예시 챌린지 칩. 탭하면 제목으로 채워진다. */
private data class ExampleChallenge(
    val emoji: String,
    val title: String,
)

private val exampleChallenges =
    listOf(
        ExampleChallenge("🏃", "주 3회 헬스장"),
        ExampleChallenge("📚", "매일 30분 독서"),
        ExampleChallenge("💧", "하루 물 2L"),
        ExampleChallenge("🧘", "매일 10분 명상"),
    )

/** 01 · 챌린지 입력. 제목·설명만 받고 나머지는 AI 추천(명세 3.1)에 맡긴다. */
@Composable
fun ChallengeInputContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
    isRecommending: Boolean = false,
) {
    val nav = LocalNavigationHelper.current
    Column(modifier = modifier.fillMaxSize().background(RuleUpTheme.colors.background)) {
        CreateChallengeTopBar(title = "새 챌린지", onBack = { nav.navigateToBack() })

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AiHelperBanner()
            TitleField(title = title, onIntent = onIntent)
            DescriptionField(description = description, onIntent = onIntent)
            ExampleChips(onIntent = onIntent)
        }

        BottomCta(isRecommending = isRecommending, onIntent = onIntent)
    }
}

/** "AI가 도와드릴게요" 그라데이션 배너. */
@Composable
private fun AiHelperBanner() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(RuleUpPalette.Indigo50, RuleUpPalette.Violet100, RuleUpPalette.Violet100),
                    ),
                ).padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(RuleUpGradients.Brand),
            contentAlignment = Alignment.Center,
        ) {
            Text("🤖", fontSize = 22.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "AI가 도와드릴게요",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "제목과 설명만 입력하면 나머지는 자동",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun TitleField(
    title: String,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "챌린지 이름",
                color = RuleUpTheme.colors.textSlate,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${title.length} / ${CreateChallengeState.TITLE_MAX}",
                color = RuleUpTheme.colors.textMuted,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(2.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.large)
                    .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = title,
                onValueChange = { onIntent(CreateChallengeIntent.SetTitle(it)) },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (title.isEmpty()) {
                        Text(
                            "예) 매일 아침 6시 기상",
                            color = RuleUpTheme.colors.textMuted,
                            fontSize = 15.sp,
                        )
                    }
                    inner()
                },
            )
        }
        Text(
            "💡 구체적인 행동을 적을수록 정확한 추천을 받아요",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun DescriptionField(
    description: String,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "설명",
                    color = RuleUpTheme.colors.textSlate,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                SmallBadge(
                    text = "선택",
                    background = RuleUpTheme.colors.surfaceVariant,
                    textColor = RuleUpTheme.colors.textSecondary,
                )
            }
            Text(
                "${description.length} / ${CreateChallengeState.DESCRIPTION_MAX}",
                color = RuleUpTheme.colors.textMuted,
                fontSize = 11.sp,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(16.dp),
        ) {
            BasicTextField(
                value = description,
                onValueChange = { onIntent(CreateChallengeIntent.SetDescription(it)) },
                textStyle =
                    TextStyle(
                        color = RuleUpTheme.colors.textSlate,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    ),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { inner ->
                    if (description.isEmpty()) {
                        Text(
                            "어떤 습관을 만들고 싶은지 적어주세요",
                            color = RuleUpTheme.colors.textMuted,
                            fontSize = 13.sp,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExampleChips(onIntent: (CreateChallengeIntent) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "예시 챌린지",
            color = RuleUpTheme.colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.66.sp,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            exampleChallenges.forEach { example ->
                Box(
                    modifier =
                        Modifier
                            .height(30.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(15.dp))
                            .clickable { onIntent(CreateChallengeIntent.SetTitle(example.title)) }
                            .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${example.emoji} ${example.title}",
                        color = RuleUpTheme.colors.textSlate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomCta(
    isRecommending: Boolean,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpGradients.Button)
                    .clickable(enabled = !isRecommending) { onIntent(CreateChallengeIntent.Recommend) },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✨", fontSize = 16.sp)
            Text(
                if (isRecommending) "추천 받는 중..." else "AI 추천 받기",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            "평균 2초 안에 추천을 받을 수 있어요",
            color = RuleUpTheme.colors.textMuted,
            fontSize = 10.sp,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChallengeInputPreview() {
    ChallengeFlowPreview {
        ChallengeInputContent(
            onIntent = {},
            title = "매일 아침 6시 기상",
            description = "아침형 인간이 되어 하루를 길게 쓰는 습관을 만들어요",
        )
    }
}
