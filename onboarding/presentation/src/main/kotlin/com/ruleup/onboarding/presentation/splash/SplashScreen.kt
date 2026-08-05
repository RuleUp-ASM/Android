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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ruleup.designsystem.singleClickable
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
    if (state.forceUpdate) {
        ForceUpdateContent(
            message = updateMessage(state.minAppVersion),
            onUpdate = { context.openPlayStore() },
        )
    } else {
        SplashContent()
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

/** 강제 업데이트 게이트 화면. 진행을 막고 스토어로만 유도한다(닫기 없음). */
@Composable
private fun ForceUpdateContent(
    message: String,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpGradients.Splash),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "업데이트가 필요해요",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Box(
                modifier =
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .singleClickable(onClick = onUpdate),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "업데이트하기",
                    color = RuleUpTheme.colors.brand,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
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
