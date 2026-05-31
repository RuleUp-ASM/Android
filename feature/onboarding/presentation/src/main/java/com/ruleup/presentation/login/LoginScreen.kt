package com.ruleup.presentation.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.ruleup.core.designsystem.component.PhoneStatusBar
import com.ruleup.core.designsystem.theme.RuleUpColors
import com.ruleup.core.designsystem.theme.RuleUpGradients

private data class SocialProvider(
    val mark: String,
    val label: String,
    val background: Color,
    val contentColor: Color,
    val markBold: Boolean = false,
    val border: Color? = null,
)

private val providers = listOf(
    SocialProvider("💬", "카카오로 시작하기", RuleUpColors.Kakao, RuleUpColors.KakaoText),
    SocialProvider("N", "네이버로 시작하기", RuleUpColors.Naver, Color.White, markBold = true),
    SocialProvider("🍎", "Apple로 시작하기", RuleUpColors.Apple, Color.White),
    SocialProvider(
        "G", "Google로 시작하기", RuleUpColors.Surface, RuleUpColors.TextPrimary,
        markBold = true, border = RuleUpColors.Border,
    ),
)

/** 05 · 로그인 화면. */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onProviderClick: (String) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(RuleUpColors.Surface),
    ) {
        PhoneStatusBar()
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(RuleUpColors.Background)
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(RuleUpGradients.Brand),
                contentAlignment = Alignment.Center,
            ) {
                Text("R", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "RuleUp에 오신 것을 환영해요",
                    color = RuleUpColors.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "1초 만에 시작할 수 있어요",
                    color = RuleUpColors.TextSecondary,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                providers.forEach { provider ->
                    SocialButton(provider, onClick = { onProviderClick(provider.label) })
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "시작과 동시에 서비스 이용약관 및 개인정보 처리방침에 동의하게 됩니다",
                color = RuleUpColors.TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SocialButton(provider: SocialProvider, onClick: () -> Unit) {
    val base = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .clip(RoundedCornerShape(12.dp))
    val withBorder = if (provider.border != null) {
        base.border(1.dp, provider.border, RoundedCornerShape(12.dp))
    } else {
        base
    }
    Row(
        modifier = withBorder
            .background(provider.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            provider.mark,
            color = provider.contentColor,
            fontSize = if (provider.markBold) 18.sp else 16.sp,
            fontWeight = if (provider.markBold) FontWeight.Bold else FontWeight.Normal,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            provider.label,
            color = provider.contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}
