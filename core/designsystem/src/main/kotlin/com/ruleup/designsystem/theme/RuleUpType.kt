package com.ruleup.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 디자인 기준은 Pretendard 지만 `res/font` 에 아직 번들되지 않아 시스템 Sans-serif 로 폴백한다.
 * 그동안 `Number*` 의 `Black`(w900)은 한글 시스템 폰트에 없어 Bold 로 떨어진다 — 숫자 강조가 얇게 나온다.
 */
val RuleUpFontFamily: FontFamily = FontFamily.SansSerif

/**
 * Figma `🎨 00 · 디자인 시스템 · 타이포 스케일`(node `1177:71`)의 21종. 이름은 `RuleUp/…` 스타일명 그대로다.
 * `number*` 는 점수·일수 전용 — 본문에 쓰면 Black 이 문장을 짓눌러 읽기 어려워진다.
 */
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

/**
 * Figma 가 21종 전부 `lineHeight 100%` · `letterSpacing 0` 이라 그대로 옮긴다.
 * 다만 한글 여러 줄에서 1.0배는 좁다 — 디자이너 의도가 아니라 Figma 기본값일 수 있어 실기기 확인이 필요하다.
 */
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
