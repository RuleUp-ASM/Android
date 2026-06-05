package com.ruleup.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** Design System v2.0 모서리 반경 토큰. */
@Immutable
data class RuleUpShapes(
    val chip: RoundedCornerShape = RoundedCornerShape(21.dp),
    val small: RoundedCornerShape = RoundedCornerShape(10.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(14.dp),
    val card: RoundedCornerShape = RoundedCornerShape(16.dp),
    val cardLarge: RoundedCornerShape = RoundedCornerShape(20.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(28.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(percent = 50),
)

val defaultRuleUpShapes = RuleUpShapes()
