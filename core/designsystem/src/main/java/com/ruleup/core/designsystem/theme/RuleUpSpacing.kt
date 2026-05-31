package com.ruleup.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Design System v2.0 간격 토큰(4pt 베이스). */
@Immutable
data class RuleUpSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 6.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    val screen: Dp = 24.dp,
)

val defaultRuleUpSpacing = RuleUpSpacing()
