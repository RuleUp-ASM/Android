package com.ruleup.profile.presentation.common

import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.domain.entity.user.Tier

/**
 * 티어 한글 표기. 서버 enum 값(`GOLD`)을 그대로 노출하지 않기 위한 것이다.
 *
 * profile 밖(탐색 카드의 최소 입장 티어 등)에서도 같은 표기가 필요해지면 `core:ui` 로 올린다 —
 * 지금은 소비자가 마이 탭뿐이라 여기 둔다.
 */
internal val Tier.label: String
    get() =
        when (this) {
            Tier.BRONZE -> "브론즈"
            Tier.SILVER -> "실버"
            Tier.GOLD -> "골드"
            Tier.DIAMOND -> "다이아"
            Tier.RUBY -> "루비"
        }

/** 구간표에서 티어를 구분하는 색 (Figma 1134:1565). 티어마다 고정이라 값이 곧 의미다. */
internal val Tier.accentColor: Color
    get() =
        when (this) {
            Tier.BRONZE -> RuleUpPalette.TextFaint
            Tier.SILVER -> RuleUpPalette.TextSub
            Tier.GOLD -> RuleUpPalette.StatusWarn
            Tier.DIAMOND -> RuleUpPalette.Primary600
            Tier.RUBY -> RuleUpPalette.StatusDanger
        }

/** "1,000 – 2,000" — 구간표의 점수 범위 표기. */
internal val Tier.scoreRangeLabel: String
    get() = "${minScore.thousandsLabel()} – ${maxScore.thousandsLabel()}"

/** 1000 → "1,000". 점수는 네 자리까지 가므로 천 단위 구분이 필요하다. */
internal fun Int.thousandsLabel(): String {
    // 음수는 구분자를 넣지 않는다 — 부호가 첫 청크에 섞여 "-,15" 가 된다.
    if (this < 0) return toString()
    return toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
