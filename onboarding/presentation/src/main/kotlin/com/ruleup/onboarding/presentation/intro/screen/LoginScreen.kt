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
import com.ruleup.designsystem.theme.RuleUpColors
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
                GoogleLoginButton(
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

/**
 * 카카오 에셋 원본 600×90. 이 비율을 놓으면 심볼과 글자가 늘어난다.
 * Google 버튼도 같은 값을 쓴다 — 가이드라인이 다른 제공사 버튼과 크기를 맞추라고 한다.
 */
private const val LOGIN_BUTTON_ASPECT_RATIO = 600f / 90f

/** 가이드라인이 정한 로고 크기. 여백 12/10/12dp 가 이 크기를 기준으로 그려져 있다. */
private val GOOGLE_LOGO_SIZE = 18.dp

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
                .aspectRatio(LOGIN_BUTTON_ASPECT_RATIO)
                .singleClickable(onClick = onClick),
    )
}

/**
 * Google 브랜딩 가이드라인(`developers.google.com/identity/branding-guidelines`)의 라이트 스펙.
 * 색·테두리·문구·여백이 전부 규정값이라 디자인 시스템 시맨틱 토큰으로 바꾸지 않는다.
 */
@Composable
private fun GoogleLoginButton(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(LOGIN_BUTTON_ASPECT_RATIO)
                .clip(RuleUpTheme.shapes.medium)
                .background(RuleUpColors.Google)
                .border(1.dp, RuleUpColors.GoogleBorder, RuleUpTheme.shapes.medium)
                .singleClickable(onClick = onClick)
                .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_logo),
            contentDescription = null,
            modifier = Modifier.size(GOOGLE_LOGO_SIZE),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            // 가이드라인이 허용하는 세 문구 중 하나. 임의로 바꾸면 브랜드 규정 위반이다.
            "Google 계정으로 로그인",
            color = RuleUpColors.GoogleText,
            // 규정은 Roboto Medium 14 — labelMedium 이 정확히 그 값이다.
            style = RuleUpTheme.typography.labelMedium,
        )
    }
}

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
