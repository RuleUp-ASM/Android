package com.ruleup.onboarding.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.onboarding.presentation.home.viewmodel.HomeIntent
import com.ruleup.onboarding.presentation.home.viewmodel.HomeViewModel
import com.ruleup.ui.component.PrimaryGradientButton
import com.ruleup.ui.theme.RuleUpTheme
import dev.zacsweers.metrox.viewmodel.metroViewModel

/** 홈. 온보딩 완료 후 진입하는 루트 화면. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        modifier = modifier,
        isLoggingOut = state.isLoggingOut,
        onIntent = viewModel::onIntent,
    )
}

@Composable
fun HomeContent(
    onIntent: (HomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    isLoggingOut: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "홈",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )

        PrimaryGradientButton(
            text = "챌린지 생성",
            modifier = Modifier.padding(top = 32.dp),
            onClick = { onIntent(HomeIntent.CreateChallenge) },
        )

        LogoutButton(
            isLoggingOut = isLoggingOut,
            modifier = Modifier.padding(top = RuleUpTheme.spacing.md),
            onClick = { onIntent(HomeIntent.Logout) },
        )
    }
}

/** 보조 스타일(테두리)의 로그아웃 버튼. 로그아웃 진행 중에는 중복 탭을 막는다. */
@Composable
private fun LogoutButton(
    isLoggingOut: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(14.dp))
                .clickable(enabled = !isLoggingOut, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (isLoggingOut) "로그아웃 중..." else "로그아웃",
            color = RuleUpTheme.colors.textSecondary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun HomeScreenPreview() {
    RuleUpTheme { HomeContent(onIntent = {}) }
}
