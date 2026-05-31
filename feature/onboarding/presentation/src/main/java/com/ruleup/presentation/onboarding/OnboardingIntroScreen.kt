package com.ruleup.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.BottomBar
import com.ruleup.core.designsystem.component.PageDots
import com.ruleup.core.designsystem.component.PhoneStatusBar
import com.ruleup.core.designsystem.component.PrimaryGradientButton
import com.ruleup.core.designsystem.theme.RuleUpTheme

/** 온보딩 한 페이지에 들어가는 내용. 02~04 화면이 이 모델만 바꿔 재사용한다. */
data class OnboardingPage(
    val emoji: String,
    val iconBackground: Brush,
    val title: String,
    val description: String,
    val buttonText: String,
    val showSkip: Boolean,
)

val onboardingPages: List<OnboardingPage> = listOf(
    OnboardingPage(
        emoji = "🎯",
        iconBackground = Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF))),
        title = "함께 정한 약속,\n혼자가 아니에요",
        description = "친구들과 그룹을 만들어\n매일 작은 도전을 함께 해나가요",
        buttonText = "다음",
        showSkip = true,
    ),
    OnboardingPage(
        emoji = "🤖",
        iconBackground = Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFECFDF5))),
        title = "AI가 인증을\n자동으로 확인해요",
        description = "사진 한 장이면 끝!\n번거로운 검증 없이 신뢰할 수 있어요",
        buttonText = "다음",
        showSkip = true,
    ),
    OnboardingPage(
        emoji = "🌡️",
        iconBackground = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFFF1F2))),
        title = "매너 온도로\n신뢰를 쌓아가요",
        description = "꾸준한 도전은 평판이 되고,\n좋은 친구들과 만날 수 있어요",
        buttonText = "시작하기",
        showSkip = false,
    ),
)

/** 02 · 03 · 04 온보딩 화면. */
@Composable
fun OnboardingIntroScreen(
    page: OnboardingPage,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    onNext: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RuleUpTheme.colors.surface),
    ) {
        PhoneStatusBar()

        // 건너뛰기 영역
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = RuleUpTheme.spacing.xxl),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (page.showSkip) {
                Text(
                    "건너뛰기",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // 본문
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(RuleUpTheme.colors.background)
                .padding(horizontal = RuleUpTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(RoundedCornerShape(120.dp))
                    .background(page.iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(page.emoji, fontSize = 110.sp)
            }
            Text(
                page.title,
                modifier = Modifier.padding(top = RuleUpTheme.spacing.xxl),
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                page.description,
                modifier = Modifier.padding(top = RuleUpTheme.spacing.xxl),
                color = RuleUpTheme.colors.textSecondary,
                style = RuleUpTheme.typography.body,
                textAlign = TextAlign.Center,
            )
        }

        // 하단: 인디케이터 + 버튼
        BottomBar {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.lg),
            ) {
                PageDots(total = onboardingPages.size, current = pageIndex)
                PrimaryGradientButton(text = page.buttonText, onClick = onNext)
            }
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingIntroScreenPreview() {
    RuleUpTheme { OnboardingIntroScreen(page = onboardingPages[0], pageIndex = 0) }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingIntroScreenLastPreview() {
    RuleUpTheme { OnboardingIntroScreen(page = onboardingPages[2], pageIndex = 2) }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingIntroScreenDarkPreview() {
    RuleUpTheme(darkTheme = true) {
        OnboardingIntroScreen(page = onboardingPages[0], pageIndex = 0)
    }
}
