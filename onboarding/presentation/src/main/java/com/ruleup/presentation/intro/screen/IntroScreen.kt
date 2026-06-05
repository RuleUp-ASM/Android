package com.ruleup.presentation.intro.screen

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** 온보딩 한 페이지에 들어가는 내용. 02~04 화면이 이 모델만 바꿔 재사용한다. */
data class OnboardingPage(
    val emoji: String,
    val iconBackground: Brush,
    val title: String,
    val description: String,
    val buttonText: String,
    val showSkip: Boolean,
)

val onboardingPages: List<OnboardingPage> =
    listOf(
        OnboardingPage(
            emoji = "🎯",
            iconBackground = Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF))),
            title = "함께 정한 약속,\n혼자가 아니에요",
            description = "친구들과 그룹을 만들어\n매일 작은 도전을 함께 해나가요",
            buttonText = "다음",
            showSkip = true,
        ),
        OnboardingPage(
            emoji = "🤖",
            iconBackground = Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFECFDF5))),
            title = "AI가 인증을\n자동으로 확인해요",
            description = "사진 한 장이면 끝!\n번거로운 검증 없이 신뢰할 수 있어요",
            buttonText = "다음",
            showSkip = true,
        ),
        OnboardingPage(
            emoji = "🌡️",
            iconBackground = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFFF1F2))),
            title = "매너 온도로\n신뢰를 쌓아가요",
            description = "꾸준한 도전은 평판이 되고,\n좋은 친구들과 만날 수 있어요",
            buttonText = "시작하기",
            showSkip = false,
        ),
    )
