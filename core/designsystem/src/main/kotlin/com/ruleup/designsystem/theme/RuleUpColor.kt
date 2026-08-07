package com.ruleup.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Figma `🎨 00 · 디자인 시스템 · 컬러 토큰`(node `1177:9`)의 변수 15개.
 *
 * **이름과 값 모두 Figma 를 따른다.** 중간에 이름을 바꾸면 다음 갱신 때 무엇이 무엇에 대응하는지
 * 다시 찾아야 한다. 화면에서 직접 쓰지 말고 [RuleUpTheme] 의 시맨틱 토큰을 통해 접근한다.
 */
object RuleUpPalette {
    // primary
    val Primary600 = Color(0xFF6C5CE7)
    val Primary300 = Color(0xFFA89DF0)
    val Primary50 = Color(0xFFF2F0FE)

    // text
    val TextInk = Color(0xFF101422)
    val TextSub = Color(0xFF4E5666)
    val TextFaint = Color(0xFF8B93A5)

    // border · bg
    val BorderLine = Color(0xFFE6E9EF)
    val BgCanvas = Color(0xFFF3F5F8)
    val BgSurface = Color(0xFFFFFFFF)

    // status
    val StatusSuccess = Color(0xFF22C55E)
    val StatusSuccessBg = Color(0xFFE7F7EF)
    val StatusDanger = Color(0xFFEF4444)
    val StatusDangerBg = Color(0xFFFDECEC)
    val StatusWarn = Color(0xFFF59E0B)
    val StatusWarnBg = Color(0xFFFFF4E2)

    /** 카카오 브랜드 색. 카카오가 정한 값이라 팔레트 교체와 무관하게 고정이다. */
    val Kakao = Color(0xFFFEE500)
    val KakaoLabel = Color(0xFF191919)
}

/**
 * 시맨틱 컬러 토큰.
 *
 * 라이트 한 벌뿐이다 — Figma 에 다크 토큰이 없다. 예전엔 다크 스킴을 따로 들고 있었는데 근거 없는
 * 값이라, 다크모드 기기에서 디자인과 다른 화면이 나갔다.
 */
data class RuleUpColorScheme(
    val brand: Color,
    val brandStrong: Color,
    val brandAccent: Color,
    val brandSoft: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textSlate: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
    val warning: Color,
    val warningContainer: Color,
)

/**
 * Figma 변수 → 시맨틱 토큰 매핑.
 *
 * [brandStrong]·[surfaceVariant]·[borderStrong]·[textSlate]·[onSuccess] 는 Figma 에 대응하는
 * 변수가 없다. 화면 84곳이 쓰고 있어 지우면 한 번에 다 고쳐야 하므로 **가장 가까운 Figma 토큰으로
 * 재지정**해 둔다. 강조 단계가 하나씩 줄지만, 디자인에 없는 색이 화면에 나가는 것보다 낫다.
 */
val LightRuleUpColors =
    RuleUpColorScheme(
        brand = RuleUpPalette.Primary600,
        // Figma 에 별도 강조 단계가 없다.
        brandStrong = RuleUpPalette.Primary600,
        brandAccent = RuleUpPalette.Primary300,
        brandSoft = RuleUpPalette.Primary50,
        background = RuleUpPalette.BgCanvas,
        surface = RuleUpPalette.BgSurface,
        // Figma 에 surface 변형이 없어 캔버스 색으로 떨어뜨린다.
        surfaceVariant = RuleUpPalette.BgCanvas,
        border = RuleUpPalette.BorderLine,
        // Figma 에 선 굵기 구분이 없다.
        borderStrong = RuleUpPalette.BorderLine,
        textPrimary = RuleUpPalette.TextInk,
        textSecondary = RuleUpPalette.TextSub,
        textMuted = RuleUpPalette.TextFaint,
        // Figma 는 본문 보조가 한 단계뿐이다.
        textSlate = RuleUpPalette.TextSub,
        success = RuleUpPalette.StatusSuccess,
        // Figma 의 StatusChip/성공 은 성공 배경 위에 같은 성공색 텍스트를 얹는다.
        onSuccess = RuleUpPalette.StatusSuccess,
        successContainer = RuleUpPalette.StatusSuccessBg,
        danger = RuleUpPalette.StatusDanger,
        dangerContainer = RuleUpPalette.StatusDangerBg,
        warning = RuleUpPalette.StatusWarn,
        warningContainer = RuleUpPalette.StatusWarnBg,
    )

/**
 * `@Composable` 컨텍스트가 아닌 곳(데이터 상수 등)에서 쓰는 정적 접근자.
 *
 * 컴포저블 안에서는 [RuleUpTheme.colors] 를 쓴다.
 */
object RuleUpColors {
    val Kakao = RuleUpPalette.Kakao
    val KakaoText = RuleUpPalette.KakaoLabel
}
