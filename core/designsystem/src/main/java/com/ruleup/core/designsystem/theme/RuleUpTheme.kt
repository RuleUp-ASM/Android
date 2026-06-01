@file:Suppress("ktlint:compose:compositionlocal-allowlist")

package com.ruleup.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
 * RuleUp Design System v2.0 테마 진입점.
 *
 * MaterialTheme 과 동일한 패턴으로, 하위 컴포저블은 [RuleUpTheme] 접근자(object)를 통해
 * 색·타이포·모양·간격 토큰을 읽는다.
 */
@Composable
fun RuleUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: RuleUpTypography = defaultRuleUpTypography,
    shapes: RuleUpShapes = defaultRuleUpShapes,
    spacing: RuleUpSpacing = defaultRuleUpSpacing,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkRuleUpColors else LightRuleUpColors
    CompositionLocalProvider(
        LocalRuleUpColors provides colors,
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
