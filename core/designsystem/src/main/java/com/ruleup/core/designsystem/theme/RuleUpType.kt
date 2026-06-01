package com.ruleup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
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

val defaultRuleUpTypography =
    RuleUpTypography(
        display = ruleUpTextStyle(FontWeight.Bold, fontSize = 40, lineHeight = 48),
        headlineLarge = ruleUpTextStyle(FontWeight.Bold, fontSize = 24, lineHeight = 32),
        headline = ruleUpTextStyle(FontWeight.Bold, fontSize = 22, lineHeight = 30),
        title = ruleUpTextStyle(FontWeight.SemiBold, fontSize = 17, lineHeight = 24),
        bodyLarge = ruleUpTextStyle(FontWeight.Normal, fontSize = 16, lineHeight = 24),
        body = ruleUpTextStyle(FontWeight.Normal, fontSize = 14, lineHeight = 22),
        label = ruleUpTextStyle(FontWeight.Medium, fontSize = 13, lineHeight = 18),
        caption = ruleUpTextStyle(FontWeight.Normal, fontSize = 11, lineHeight = 16),
        overline = ruleUpTextStyle(FontWeight.SemiBold, fontSize = 11, lineHeight = 16, letterSpacing = 0.66),
    )

private fun ruleUpTextStyle(
    fontWeight: FontWeight,
    fontSize: Int,
    lineHeight: Int,
    letterSpacing: Double? = null,
): TextStyle =
    TextStyle(
        fontFamily = RuleUpFontFamily,
        fontWeight = fontWeight,
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = letterSpacing?.sp ?: TextUnit.Unspecified,
    )
