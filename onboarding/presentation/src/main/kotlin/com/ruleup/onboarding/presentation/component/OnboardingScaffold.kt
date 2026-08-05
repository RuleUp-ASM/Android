package com.ruleup.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.component.PrimaryGradientButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme

/** 온보딩 전체 단계 수. 화면·진행바·로깅이 같은 값을 봐야 해서 한곳에 둔다. */
const val ONBOARDING_TOTAL_STEPS = 6

/**
 * 온보딩 6단계 공통 골격 — AppBar(뒤로 + n/6) · 진행바 · 본문 · 하단 CTA.
 *
 * 진행 표시를 점(dot)에서 **막대**로 바꿨다. 6단계에서 점을 쓰면 지금 어디쯤인지 한눈에 안 들어오고,
 * 디자인도 채워지는 막대다.
 *
 * @param step 1부터 센다. 화면 문구·진행률과 같은 기준이라 0-based 로 두면 매번 ±1 실수가 난다.
 * @param nextEnabled false 면 CTA 를 흐리게 두고 눌러도 넘어가지 않는다. 유효하지 않은 입력으로
 *   전진하면 마지막 제출에서야 서버가 튕겨, 사용자가 되짚어야 할 단계가 멀어진다.
 */
@Composable
fun OnboardingScaffold(
    step: Int,
    buttonText: String,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.surface)
                .imePadding(),
    ) {
        OnboardingTopBar(step = step, onBack = onBack)
        OnboardingProgress(step = step)
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            content()
        }
        BottomBar {
            PrimaryGradientButton(
                text = buttonText,
                modifier = Modifier.alpha(if (nextEnabled) 1f else DISABLED_ALPHA),
                height = 56,
                onClick = { if (nextEnabled) onNext() },
            )
        }
    }
}

/** 뒤로 + 우측 `n/6`. 진행 상황은 스크린리더가 읽도록 라벨을 준다. */
@Composable
private fun OnboardingTopBar(
    step: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(22.dp)
                    .singleClickable(onClick = onBack)
                    .semantics { contentDescription = "이전 단계로" },
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = RuleUpTheme.colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = "$step/$ONBOARDING_TOTAL_STEPS",
            modifier = Modifier.semantics { contentDescription = "전체 $ONBOARDING_TOTAL_STEPS 단계 중 $step 단계" },
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 현재 단계 비율만큼 채워지는 3dp 막대. */
@Composable
private fun OnboardingProgress(step: Int) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 20.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RuleUpTheme.colors.borderStrong),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(step.toFloat() / ONBOARDING_TOTAL_STEPS)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(RuleUpGradients.Indicator),
            )
        }
    }
}

private const val DISABLED_ALPHA = 0.4f
