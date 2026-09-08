package com.ruleup.onboarding.presentation.intro.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.domain.auth.entity.OAuthProvider
import com.ruleup.onboarding.presentation.R
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
                // 브랜드 로크업. Figma 타입 스케일(최대 44)에 없는 크기라 리터럴로 둔다.
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
                    style = RuleUpTheme.typography.body,
                )
            }

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KakaoLoginButton(
                    onClick = { onIntent(LoginIntent.LoginClicked(OAuthProvider.KAKAO)) },
                )
                SocialButton(
                    googleProvider(),
                    onClick = { onIntent(LoginIntent.LoginClicked(OAuthProvider.GOOGLE)) },
                )
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

/** 에셋 원본 600×90. 이 비율을 놓으면 심볼과 글자가 늘어난다. */
private const val KAKAO_BUTTON_ASPECT_RATIO = 600f / 90f

/**
 * 카카오 공식 버튼 에셋(`kakao_login_large_wide`). 카카오 로그인 디자인 가이드가 에셋 변형을
 * 금지해서 색·문구·심볼을 코드로 다시 그리지 않고 이미지를 그대로 건다.
 */
@Composable
private fun KakaoLoginButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.kakao_login_large_wide),
        // 문구가 이미지 안에 있어 스크린리더가 못 읽는다.
        contentDescription = "카카오 로그인",
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(KAKAO_BUTTON_ASPECT_RATIO)
                .singleClickable(onClick = onClick),
    )
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
            style = if (provider.markBold) RuleUpTheme.typography.section else RuleUpTheme.typography.labelMedium,
        )
        Spacer(Modifier.size(10.dp))
        Text(
            provider.label,
            color = provider.contentColor,
            style = RuleUpTheme.typography.cardTitle,
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
 * 코드로 그리는 건 Google 버튼뿐이다 — 카카오는 공식 에셋이라 [KakaoLoginButton] 이 맡는다.
 * surface/text/border 를 쓰므로 테마에 따라 라이트·다크로 바뀐다.
 */
@Composable
private fun googleProvider(): SocialProvider =
    SocialProvider(
        "G",
        "Google로 시작하기",
        RuleUpTheme.colors.surface,
        RuleUpTheme.colors.textPrimary,
        markBold = true,
        border = RuleUpTheme.colors.border,
        provider = OAuthProvider.GOOGLE,
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
