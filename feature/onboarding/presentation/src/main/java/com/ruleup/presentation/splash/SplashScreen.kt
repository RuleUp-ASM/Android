package com.ruleup.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.PhoneStatusBar
import com.ruleup.core.designsystem.theme.RuleUpGradients
import com.ruleup.core.designsystem.theme.RuleUpTheme

/** 01 · 스플래시 화면. */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RuleUpGradients.Splash),
    ) {
        PhoneStatusBar(contentColor = Color.White)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("R", color = Color.White, fontSize = 64.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier.padding(top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
            ) {
                Text("RuleUp", color = Color.White, style = RuleUpTheme.typography.display)
                Text(
                    "함께 약속, 함께 성장",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Row(
                modifier = Modifier.padding(top = 44.dp),
                horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
            ) {
                LoadingDot(Color.White.copy(alpha = 0.35f))
                LoadingDot(Color.White.copy(alpha = 0.65f))
                LoadingDot(Color.White)
            }
        }
    }
}

@Composable
private fun LoadingDot(color: Color) {
    Box(
        Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
    )
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SplashScreenPreview() {
    RuleUpTheme { SplashScreen() }
}
