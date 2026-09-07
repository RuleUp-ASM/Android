@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.ruleup.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalRuleUpColors =
    staticCompositionLocalOf<RuleUpColorScheme> {
        error("RuleUpColorScheme is not provided. Wrap your content in RuleUpTheme { }.")
    }
private val LocalRuleUpTypography = staticCompositionLocalOf { defaultRuleUpTypography }
private val LocalRuleUpShapes = staticCompositionLocalOf { defaultRuleUpShapes }
private val LocalRuleUpSpacing = staticCompositionLocalOf { defaultRuleUpSpacing }

/**
 * 테마 진입점. 하위 컴포저블은 [RuleUpTheme] 접근자로 토큰을 읽는다.
 * 라이트 고정이다 — Figma 에 다크 토큰이 없어, 시스템 설정을 따라가면 근거 없는 색이 다크 기기에만 나간다.
 */
@Composable
fun RuleUpTheme(
    typography: RuleUpTypography = defaultRuleUpTypography,
    shapes: RuleUpShapes = defaultRuleUpShapes,
    spacing: RuleUpSpacing = defaultRuleUpSpacing,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRuleUpColors provides LightRuleUpColors,
        LocalRuleUpTypography provides typography,
        LocalRuleUpShapes provides shapes,
        LocalRuleUpSpacing provides spacing,
        content = content,
    )
}

/** MaterialTheme 스타일의 토큰 접근자. 예: `RuleUpTheme.colors.brand`. */
object RuleUpTheme {
    val colors: RuleUpColorScheme
        @Composable @ReadOnlyComposable
        get() = LocalRuleUpColors.current
    val typography: RuleUpTypography
        @Composable @ReadOnlyComposable
        get() = LocalRuleUpTypography.current
    val shapes: RuleUpShapes
        @Composable @ReadOnlyComposable
        get() = LocalRuleUpShapes.current
    val spacing: RuleUpSpacing
        @Composable @ReadOnlyComposable
        get() = LocalRuleUpSpacing.current
}
