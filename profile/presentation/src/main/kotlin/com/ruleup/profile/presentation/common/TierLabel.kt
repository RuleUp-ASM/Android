package com.ruleup.profile.presentation.common

import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.domain.entity.user.Tier

/** 티어 한글 표기. */
internal val Tier.label: String
    get() =
        when (this) {
            Tier.BRONZE -> "브론즈"
            Tier.SILVER -> "실버"
            Tier.GOLD -> "골드"
            Tier.DIAMOND -> "다이아"
            Tier.RUBY -> "루비"
        }

/** 구간표에서 티어를 구분하는 색. */
internal val Tier.accentColor: Color
    get() =
        when (this) {
            Tier.BRONZE -> RuleUpPalette.TextFaint
            Tier.SILVER -> RuleUpPalette.TextSub
            Tier.GOLD -> RuleUpPalette.StatusWarn
            Tier.DIAMOND -> RuleUpPalette.Primary600
            Tier.RUBY -> RuleUpPalette.StatusDanger
        }

/** 티어 점수 구간 표기. */
internal val Tier.scoreRangeLabel: String
    get() = "${minScore.thousandsLabel()} – ${maxScore.thousandsLabel()}"

/** 1000 → "1,000". */
internal fun Int.thousandsLabel(): String {
    // 음수는 구분자를 넣지 않는다
    if (this < 0) return toString()
    return toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
