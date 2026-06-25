package com.ruleup.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Design System v2.0 의 브랜드 그라데이션 토큰. */
object RuleUpGradients {
    /** 스플래시 배경: indigo → violet → light violet. */
    val Splash =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Indigo500, RuleUpPalette.Violet500, RuleUpPalette.Violet400),
        )

    /** 기본 CTA 버튼: indigo → indigo600. */
    val Button =
        Brush.horizontalGradient(
            colors = listOf(RuleUpPalette.Indigo500, RuleUpPalette.Indigo600),
        )

    /** 브랜드 뱃지 / 아바타: 45° indigo → violet. */
    val Brand =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Indigo500, RuleUpPalette.Violet500),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    /** 액센트(따뜻한 톤): amber → rose. */
    val Warm =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Amber500, RuleUpPalette.Rose500),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    /** 진행 인디케이터 활성 바: indigo → violet. */
    val Indicator =
        Brush.horizontalGradient(
            colors = listOf(RuleUpPalette.Indigo500, RuleUpPalette.Violet500),
        )

    /** 임의의 두 색으로 45° 그라데이션을 만든다(이니셜 팔레트 등). */
    fun diagonal(
        start: Color,
        end: Color,
    ): Brush = Brush.linearGradient(colors = listOf(start, end), start = Offset.Zero, end = Offset.Infinite)
}
