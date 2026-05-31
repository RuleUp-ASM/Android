package com.ruleup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.theme.RuleUpGradients
import com.ruleup.core.designsystem.theme.RuleUpTheme

/** 프로필 설정 온보딩 상단 내비게이션: 뒤로(‹) · N단계 진행 dot · 건너뛰기. */
@Composable
fun ProfileSetupTopBar(
    currentStep: Int,
    modifier: Modifier = Modifier,
    totalSteps: Int = 4,
    onBack: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(RuleUpTheme.colors.surface)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = RuleUpTheme.colors.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
        StepDots(currentStep = currentStep, totalSteps = totalSteps)
        Text(
            "건너뛰기",
            modifier = Modifier.clickable(onClick = onSkip),
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 지나온 단계는 옅은 indigo, 현재 단계는 그라데이션 바, 남은 단계는 회색. */
@Composable
private fun StepDots(currentStep: Int, totalSteps: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            when {
                index == currentStep -> Box(
                    Modifier
                        .height(6.dp)
                        .width(24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RuleUpGradients.Indicator),
                )

                index < currentStep -> Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RuleUpTheme.colors.brand.copy(alpha = 0.4f)),
                )

                else -> Box(
                    Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(RuleUpTheme.colors.borderStrong),
                )
            }
        }
    }
}

/**
 * 프로필 설정 화면 공통 골격: 상태바 + 상단바 + 스크롤 본문 + 하단 CTA.
 */
@Composable
fun ProfileSetupScaffold(
    step: Int,
    buttonText: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSkip: () -> Unit = {},
    onNext: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RuleUpTheme.colors.surface),
    ) {
        PhoneStatusBar()
        ProfileSetupTopBar(currentStep = step, onBack = onBack, onSkip = onSkip)
        Column(
            modifier = Modifier
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
            PrimaryGradientButton(text = buttonText, height = 56, onClick = onNext)
        }
    }
}
