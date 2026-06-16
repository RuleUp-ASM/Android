package com.ruleup.onboarding.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.onboarding.presentation.splash.viewmodel.SplashIntent
import com.ruleup.onboarding.presentation.splash.viewmodel.SplashViewModel
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun SplashScreen(viewModel: SplashViewModel = metroViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.onIntent(SplashIntent.Check)
    }
    SplashContent()
}

@Composable
private fun SplashContent(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpGradients.Splash),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            // 로고 박스 (흰 카드 + "R")
            Box(
                modifier =
                    Modifier
                        .height(120.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "R",
                    color = RuleUpTheme.colors.brand,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // 타이틀 + 서브카피
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "RuleUp",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    text = "함께 약속, 함께 성장",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            // 진행 점 3개
            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(3) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    RuleUpTheme { SplashContent() }
}
