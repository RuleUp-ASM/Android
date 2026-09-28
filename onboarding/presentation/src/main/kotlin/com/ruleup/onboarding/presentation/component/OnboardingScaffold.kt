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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.component.RuleUpPrimaryButton
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.domain.logging.OnboardingEvents
import com.ruleup.onboarding.domain.logging.OnboardingStep
import com.ruleup.ui.helper.LocalBizLogger

/** 온보딩 전체 단계 수. */
const val ONBOARDING_TOTAL_STEPS = 6

/** 온보딩 6단계 공통 골격 */
@Composable
fun OnboardingScaffold(
    step: OnboardingStep,
    buttonText: String,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
    // 그 단계에서 아무것도 고르지 않고 넘어갔는지.
    skipped: Boolean = false,
    onBack: () -> Unit = {},
    onNext: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val bizLogger = LocalBizLogger.current
    // 단계 진입·완료 로깅을 여기서 한다.
    LaunchedEffect(step) {
        bizLogger.record(OnboardingEvents.stepView(step))
    }
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.surface)
                .imePadding(),
    ) {
        OnboardingTopBar(step = step.index, onBack = onBack)
        OnboardingProgress(step = step.index)
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
            RuleUpPrimaryButton(
                text = buttonText,
                modifier = Modifier.alpha(if (nextEnabled) 1f else DISABLED_ALPHA),
                onClick = {
                    if (!nextEnabled) return@RuleUpPrimaryButton
                    bizLogger.record(OnboardingEvents.stepComplete(step, skipped))
                    onNext()
                },
            )
        }
    }
}

/** 뒤로 + 우측 `n/6`. */
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
            Text("‹", color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.title)
        }
        Text(
            text = "$step/$ONBOARDING_TOTAL_STEPS",
            modifier = Modifier.semantics { contentDescription = "전체 $ONBOARDING_TOTAL_STEPS 단계 중 $step 단계" },
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.smallMedium,
        )
    }
}

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

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun OnboardingScaffoldPreview() {
    RuleUpTheme {
        com.ruleup.ui.helper.PreviewEnvironment {
            OnboardingScaffold(
                step =
                    com.ruleup.onboarding.domain.logging.OnboardingStep.entries
                        .first(),
                buttonText = "다음",
                content = {
                    Text("프로필을 설정해 주세요")
                },
            )
        }
    }
}
