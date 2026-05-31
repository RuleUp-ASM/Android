package com.ruleup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Design System v2.0 타이포 스케일.
 *
 * 디자인 기준 폰트는 Pretendard(한글) + Inter(영문)이다. 폰트 리소스(res/font)가 번들되면
 * [RuleUpFontFamily] 를 교체하면 되고, 그 전까지는 시스템 Sans-serif 로 폴백한다.
 */
val RuleUpFontFamily: FontFamily = FontFamily.SansSerif

@Immutable
data class RuleUpTypography(
    val display: TextStyle,
    val headlineLarge: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val bodyLarge: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle,
)

val defaultRuleUpTypography = RuleUpTypography(
    display = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headline = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 30.sp),
    title = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    body = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    label = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    caption = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp),
    overline = TextStyle(fontFamily = RuleUpFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.66.sp),
)
