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

/** 테마 진입점. */
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

/** MaterialTheme 스타일의 토큰 접근자. */
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
