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
 * 테마 진입점.
 *
 * MaterialTheme 과 동일한 패턴으로, 하위 컴포저블은 [RuleUpTheme] 접근자(object)를 통해
 * 색·타이포·모양·간격 토큰을 읽는다.
 *
 * **라이트 고정이다.** 다크 스킴을 받지 않는 이유는 Figma 에 다크 토큰이 없어서다 — 예전엔
 * 시스템 설정을 따라갔는데, 그 값들이 디자인에 근거가 없어 다크모드 기기만 다른 화면을 봤다.
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
