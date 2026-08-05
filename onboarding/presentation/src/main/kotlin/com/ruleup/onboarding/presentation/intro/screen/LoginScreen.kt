package com.ruleup.onboarding.presentation.intro.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpColors
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.domain.auth.entity.OAuthProvider
import com.ruleup.onboarding.presentation.common.AuthFailureHost
import com.ruleup.onboarding.presentation.common.AuthFailureUi
import com.ruleup.onboarding.presentation.intro.viewmodel.LoginEffect
import com.ruleup.onboarding.presentation.intro.viewmodel.LoginIntent
import com.ruleup.onboarding.presentation.intro.viewmodel.LoginViewModel
import com.ruleup.onboarding.presentation.oauth.rememberOAuthLauncher
import com.ruleup.ui.helper.LocalMessageHelper

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val messageHelper = LocalMessageHelper.current
    var failure by remember { mutableStateOf<AuthFailureUi?>(null) }
    val launcher =
        rememberOAuthLauncher { result ->
            result
                .onSuccess { viewModel.onIntent(LoginIntent.AuthorizationReceived(it)) }
                .onFailure { viewModel.onIntent(LoginIntent.AuthFailed(it)) }
        }

    LaunchedEffect(Unit) {
        viewModel.onIntent(LoginIntent.Load)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginEffect.LaunchOAuth -> launcher.launch(effect.provider)
                is LoginEffect.ShowFailure ->
                    when (val ui = effect.ui) {
                        is AuthFailureUi.Toast -> messageHelper.showToast(ui.message)
                        else -> failure = ui
                    }
            }
        }
    }
    LoginContent(
        onIntent = viewModel::onIntent,
    )
    AuthFailureHost(ui = failure, onDismiss = { failure = null })
}

@Composable
fun LoginContent(
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.surface),
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(RuleUpTheme.colors.background)
                    .padding(horizontal = RuleUpTheme.spacing.xxl, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xl),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(RuleUpGradients.Brand),
                contentAlignment = Alignment.Center,
            ) {
                Text("R", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
            ) {
                Text(
                    "RuleUp에 오신 것을 환영해요",
                    color = RuleUpTheme.colors.textPrimary,
                    style = RuleUpTheme.typography.title,
                )
                Text(
                    "1초 만에 시작할 수 있어요",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                socialProviders().forEach { provider ->
                    SocialButton(
                        provider,
                        onClick = { onIntent(LoginIntent.LoginClicked(provider.provider)) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                "시작과 동시에 서비스 이용약관 및 개인정보 처리방침에 동의하게 됩니다",
                color = RuleUpTheme.colors.textMuted,
                style = RuleUpTheme.typography.caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SocialButton(
    provider: SocialProvider,
    onClick: () -> Unit,
) {
    val base =
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RuleUpTheme.shapes.medium)
    val withBorder =
        if (provider.border != null) {
            base.border(1.dp, provider.border, RuleUpTheme.shapes.medium)
        } else {
            base
        }
    Row(
        modifier =
            withBorder
                .background(provider.background)
                .singleClickable(onClick = onClick)
                .padding(horizontal = RuleUpTheme.spacing.lg),
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

private data class SocialProvider(
    val mark: String,
    val label: String,
    val background: Color,
    val contentColor: Color,
    val markBold: Boolean = false,
    val border: Color? = null,
    val provider: OAuthProvider,
)

/**
 * 소셜 로그인 버튼 목록. 카카오는 브랜드 고정색이라 상수지만, Google 버튼은 surface/text/border 를
 * 쓰므로 테마에 따라 라이트·다크로 바뀐다.
 *
 * 네이버·Apple 은 MVP 범위 밖이라 뺐다. OAuthActivity 가 구현한 건 카카오·구글뿐이라 눌러도
 * "미지원 provider" 로 끝나던 버튼들이다.
 */
@Composable
private fun socialProviders(): List<SocialProvider> =
    listOf(
        SocialProvider(
            "💬",
            "카카오로 시작하기",
            RuleUpColors.Kakao,
            RuleUpColors.KakaoText,
            provider = OAuthProvider.KAKAO,
        ),
        SocialProvider(
            "G",
            "Google로 시작하기",
            RuleUpTheme.colors.surface,
            RuleUpTheme.colors.textPrimary,
            markBold = true,
            border = RuleUpTheme.colors.border,
            provider = OAuthProvider.GOOGLE,
        ),
    )

@Preview
@Composable
private fun LoginScreenPreview() {
    RuleUpTheme { LoginContent(onIntent = {}) }
}

@Preview
@Composable
private fun LoginScreenDarkPreview() {
    RuleUpTheme { LoginContent(onIntent = {}) }
}
