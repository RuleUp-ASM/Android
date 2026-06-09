package com.ruleup.onboarding.presentation.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.onboarding.presentation.splash.viewmodel.SplashIntent
import com.ruleup.onboarding.presentation.splash.viewmodel.SplashViewModel
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

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
                .background(RuleUpGradients.Brand),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                text = "RuleUp",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun SplashScreenPreview() {
    RuleUpTheme { SplashContent() }
}
