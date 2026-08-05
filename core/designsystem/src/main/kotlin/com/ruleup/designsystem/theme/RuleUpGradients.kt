package com.ruleup.designsystem.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 브랜드 그라데이션.
 *
 * **Figma 디자인 시스템에는 그라데이션 토큰이 없다.** 스플래시 배경과 브랜드 뱃지가 쓰고 있어
 * 남겨 두되, 색은 새 팔레트(`primary/600` → `primary/300`)로 맞춘다. 디자인이 정해지면 정리한다.
 *
 * CTA 버튼 그라데이션은 없앴다 — Figma `Button/Primary` 는 단색 `primary/600` 이다.
 * [com.ruleup.designsystem.component.RuleUpPrimaryButton] 을 쓴다.
 */
object RuleUpGradients {
    /** 스플래시 배경. */
    val Splash =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300),
        )

    /** 브랜드 뱃지 / 아바타: 45°. */
    val Brand =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    /** 액센트(따뜻한 톤): warn → danger. */
    val Warm =
        Brush.linearGradient(
            colors = listOf(RuleUpPalette.StatusWarn, RuleUpPalette.StatusDanger),
            start = Offset.Zero,
            end = Offset.Infinite,
        )

    /** 진행 인디케이터 활성 바. */
    val Indicator =
        Brush.horizontalGradient(
            colors = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300),
        )

    /** 임의의 두 색으로 45° 그라데이션을 만든다(이니셜 팔레트 등). */
    fun diagonal(
        start: Color,
        end: Color,
    ): Brush = Brush.linearGradient(colors = listOf(start, end), start = Offset.Zero, end = Offset.Infinite)
}
