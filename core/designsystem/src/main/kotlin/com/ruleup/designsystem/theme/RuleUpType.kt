package com.ruleup.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 기본 글꼴: 시스템 Sans-serif. */
val RuleUpFontFamily: FontFamily = FontFamily.SansSerif

/** 타이포그래피 토큰. */
@Immutable
data class RuleUpTypography(
    val numberXl: TextStyle,
    val numberL: TextStyle,
    val title: TextStyle,
    val numberM: TextStyle,
    val numberS: TextStyle,
    val section: TextStyle,
    val cardTitle: TextStyle,
    val labelMedium: TextStyle,
    val body: TextStyle,
    val bodyBold: TextStyle,
    val bodyMedium: TextStyle,
    val small: TextStyle,
    val smallBold: TextStyle,
    val smallMedium: TextStyle,
    val caption: TextStyle,
    val captionBold: TextStyle,
    val captionMedium: TextStyle,
    val tiny: TextStyle,
    val tinyMedium: TextStyle,
    val tinyBold: TextStyle,
    val micro: TextStyle,
)

val defaultRuleUpTypography =
    RuleUpTypography(
        numberXl = ruleUpTextStyle(FontWeight.Black, size = 44),
        numberL = ruleUpTextStyle(FontWeight.Black, size = 28),
        title = ruleUpTextStyle(FontWeight.Bold, size = 22),
        numberM = ruleUpTextStyle(FontWeight.Black, size = 20),
        numberS = ruleUpTextStyle(FontWeight.Black, size = 16),
        section = ruleUpTextStyle(FontWeight.Bold, size = 15),
        cardTitle = ruleUpTextStyle(FontWeight.Bold, size = 14),
        labelMedium = ruleUpTextStyle(FontWeight.Medium, size = 14),
        body = ruleUpTextStyle(FontWeight.Normal, size = 13),
        bodyBold = ruleUpTextStyle(FontWeight.Bold, size = 13),
        bodyMedium = ruleUpTextStyle(FontWeight.Medium, size = 13),
        small = ruleUpTextStyle(FontWeight.Normal, size = 12),
        smallBold = ruleUpTextStyle(FontWeight.Bold, size = 12),
        smallMedium = ruleUpTextStyle(FontWeight.Medium, size = 12),
        caption = ruleUpTextStyle(FontWeight.Normal, size = 11),
        captionBold = ruleUpTextStyle(FontWeight.Bold, size = 11),
        captionMedium = ruleUpTextStyle(FontWeight.Medium, size = 11),
        tiny = ruleUpTextStyle(FontWeight.Normal, size = 10),
        tinyMedium = ruleUpTextStyle(FontWeight.Medium, size = 10),
        tinyBold = ruleUpTextStyle(FontWeight.Bold, size = 10),
        micro = ruleUpTextStyle(FontWeight.Bold, size = 9),
    )

/** 행간 100%, 자간 0. */
private fun ruleUpTextStyle(
    fontWeight: FontWeight,
    size: Int,
): TextStyle =
    TextStyle(
        fontFamily = RuleUpFontFamily,
        fontWeight = fontWeight,
        fontSize = size.sp,
        lineHeight = size.sp,
    )
