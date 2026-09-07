package com.ruleup.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 브랜드 그라데이션. Figma 에 그라데이션 토큰이 없어 색만 새 팔레트에 맞춰 남겨 둔 것이다.
 * CTA 버튼에는 쓰지 않는다 — Figma `Button/Primary` 는 단색이고, `RuleUpPrimaryButton` 을 쓴다.
 */
object RuleUpGradients {
    val Splash =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300),
        )

    val Brand =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    val Warm =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.StatusWarn, RuleUpPalette.StatusDanger),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    val Indicator =
        Brush.horizontalGradient(
            colors = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300),
        )

    fun diagonal(
        start: Color,
        end: Color,
    ): Brush = Brush.linearGradient(colors = listOf(start, end), start = Offset.Zero, end = Offset.Infinite)
}
