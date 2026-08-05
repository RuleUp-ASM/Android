package com.ruleup.onboarding.presentation.splash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.presentation.splash.viewmodel.SplashIntent
import com.ruleup.onboarding.presentation.splash.viewmodel.SplashViewModel

@Composable
fun SplashScreen(viewModel: SplashViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.onIntent(SplashIntent.Check)
    }
    // 스플래시는 항상 그린다. 강제 업데이트는 그 위에 얹는 다이얼로그다 — 화면을 갈아치우면
    // 인트로 응답이 도착하는 순간 로고가 사라졌다 나타난다.
    SplashContent()
    if (state.forceUpdate) {
        ForceUpdateDialog(
            message = updateMessage(state.minAppVersion),
            onUpdate = { context.openPlayStore() },
        )
    }
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

/**
 * 강제 업데이트 다이얼로그.
 *
 * **닫을 수 없다** — 뒤로가기·바깥 탭으로 닫히면 게이트를 우회해 구버전으로 앱을 계속 쓰게 된다.
 * 그래서 `onDismissRequest` 를 비우고 [DialogProperties] 로 두 경로를 모두 막는다. 버튼도 스토어
 * 이동 하나뿐이다.
 */
@Composable
private fun ForceUpdateDialog(
    message: String,
    onUpdate: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = {
            Text(
                text = "업데이트가 필요해요",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                text = message,
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(
                    text = "업데이트하기",
                    color = RuleUpTheme.colors.brand,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        containerColor = RuleUpTheme.colors.surface,
    )
}

/**
 * 강제 업데이트 안내 문구.
 *
 * 서버가 최소 버전을 주면 그 값을 그대로 보여준다 — 명세가 이 필드를 "x.x.x 이상으로 업데이트해
 * 주세요" 표시용으로 정의한다. 못 받았을 때만 일반 문구로 떨어진다.
 */
private fun updateMessage(minAppVersion: String?): String =
    minAppVersion
        ?.takeIf { it.isNotBlank() }
        ?.let { "$it 이상으로 업데이트해 주세요." }
        ?: "원활한 사용을 위해 최신 버전으로 업데이트해주세요."

/** Play 스토어 상세로 이동. 스토어 앱이 없으면 웹으로 폴백. */
private fun Context.openPlayStore() {
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
    runCatching { startActivity(market) }.onFailure {
        if (it is ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    "https://play.google.com/store/apps/details?id=$packageName".toUri(),
                ),
            )
        }
    }
}

@Preview
@Composable
private fun SplashScreenPreview() {
    RuleUpTheme { SplashContent() }
}

@Preview
@Composable
private fun ForceUpdatePreview() {
    RuleUpTheme {
        SplashContent()
        ForceUpdateDialog(message = updateMessage("1.2.0"), onUpdate = {})
    }
}
