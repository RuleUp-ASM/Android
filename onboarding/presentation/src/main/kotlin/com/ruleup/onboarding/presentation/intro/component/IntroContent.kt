package com.ruleup.onboarding.presentation.intro.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.onboarding.domain.IntroTrustPage
import com.ruleup.onboarding.domain.IntroVerifyPage
import com.ruleup.onboarding.domain.LoginPage
import com.ruleup.onboarding.presentation.intro.screen.OnboardingPage
import com.ruleup.onboarding.presentation.intro.screen.onboardingPages
import com.ruleup.ui.component.BottomBar
import com.ruleup.ui.component.PageDots
import com.ruleup.ui.component.PrimaryGradientButton
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.theme.RuleUpTheme

/** 02 · 03 · 04 온보딩 화면. */
@Composable
fun IntroContent(
    page: OnboardingPage,
    pageIndex: Int,
    modifier: Modifier = Modifier,
) {
    val navigationHelper = LocalNavigationHelper.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.surface),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = RuleUpTheme.spacing.xxl),
            contentAlignment = Alignment.CenterEnd,
        ) {
            if (page.showSkip) {
                Text(
                    "건너뛰기",
                    modifier =
                        Modifier.clickable {
                            navigationHelper.navigateTo(IntroTrustPage)
                        },
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // 본문
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.background)
                    .padding(horizontal = RuleUpTheme.spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier =
                    Modifier
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

        // 하단: 인디케이터 + 버튼 (버튼이 화면 맨 아래에 붙지 않도록 하단 여백을 넉넉히 준다)
        BottomBar(
            padding =
                PaddingValues(
                    start = RuleUpTheme.spacing.xl,
                    end = RuleUpTheme.spacing.xl,
                    top = RuleUpTheme.spacing.xl,
                    bottom = RuleUpTheme.spacing.xxxl,
                ),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.lg),
            ) {
                PageDots(total = onboardingPages.size, current = pageIndex)
                PrimaryGradientButton(text = page.buttonText, onClick = {
                    when (pageIndex) {
                        0 -> navigationHelper.navigateTo(IntroVerifyPage)
                        1 -> navigationHelper.navigateTo(IntroTrustPage)
                        2 -> navigationHelper.navigateTo(LoginPage)
                    }
                })
            }
        }
    }
}
