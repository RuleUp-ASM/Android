package com.ruleup.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * RuleUp Design System v2.0 — "Modern Minimal · Indigo Brand · Cool Slate Neutrals".
 *
 * 01 · Brand Identity 섹션의 원시(raw) 팔레트. 화면에서 직접 쓰기보다
 * [RuleUpColorScheme] / [RuleUpTheme] 의 시맨틱 토큰을 통해 접근하는 것을 권장한다.
 */
object RuleUpPalette {
    // Brand · Indigo
    val Indigo50 = Color(0xFFEEF2FF)
    val Indigo100 = Color(0xFFE0E7FF)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo700 = Color(0xFF4338CA)

    // Brand · Violet
    val Violet100 = Color(0xFFF5F3FF)
    val Violet400 = Color(0xFFA78BFA)
    val Violet500 = Color(0xFF8B5CF6)

    // Neutral · Cool Slate
    val Slate900 = Color(0xFF0F172A)
    val Slate700 = Color(0xFF334155)
    val Slate600 = Color(0xFF475569)
    val Slate500 = Color(0xFF64748B)
    val Slate400 = Color(0xFF94A3B8)
    val Slate300 = Color(0xFFCBD5E1)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate50 = Color(0xFFF8FAFC)
    val White = Color(0xFFFFFFFF)

    // Semantic
    val Green500 = Color(0xFF10B981)
    val Green700 = Color(0xFF047857)
    val GreenTint = Color(0xFFF0FDF4)
    val Amber500 = Color(0xFFF59E0B)
    val AmberTint = Color(0xFFFFFBEB)
    val Rose500 = Color(0xFFF43F5E)
    val Rose700 = Color(0xFFBE123C)
    val Cyan500 = Color(0xFF06B6D4)
    val Blue500 = Color(0xFF3B82F6)

    // Brand · Social
    val Kakao = Color(0xFFFEE500)
    val KakaoLabel = Color(0xFF191919)
    val Naver = Color(0xFF03C75A)
    val Apple = Color(0xFF000000)
}

/**
 * 시맨틱 컬러 토큰. Light / Dark 두 스킴을 모두 제공한다(Design System v2.0 "Light + Dark Ready").
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
    val warningContainer: Color,
    val isDark: Boolean,
)

val LightRuleUpColors =
    RuleUpColorScheme(
        brand = RuleUpPalette.Indigo500,
        brandStrong = RuleUpPalette.Indigo600,
        brandAccent = RuleUpPalette.Violet500,
        brandSoft = RuleUpPalette.Indigo50,
        background = RuleUpPalette.Slate50,
        surface = RuleUpPalette.White,
        surfaceVariant = RuleUpPalette.Slate100,
        border = RuleUpPalette.Slate200,
        borderStrong = RuleUpPalette.Slate300,
        textPrimary = RuleUpPalette.Slate900,
        textSecondary = RuleUpPalette.Slate500,
        textMuted = RuleUpPalette.Slate400,
        textSlate = RuleUpPalette.Slate600,
        success = RuleUpPalette.Green500,
        onSuccess = RuleUpPalette.Green700,
        successContainer = RuleUpPalette.GreenTint,
        danger = RuleUpPalette.Rose500,
        warningContainer = RuleUpPalette.AmberTint,
        isDark = false,
    )

/**
 * Light 스킴 시맨틱 색을 정적으로 노출하는 편의 객체.
 *
 * `@Composable` 컨텍스트가 아닌 곳(데이터 상수 등)이나 단순 화면에서 가볍게 쓰기 위한 용도.
 * 테마 전환(다크 모드)이 필요한 곳에서는 [RuleUpTheme.colors] 를 사용한다.
 */
object RuleUpColors {
    val Indigo = RuleUpPalette.Indigo500
    val Indigo700 = RuleUpPalette.Indigo600
    val Purple = RuleUpPalette.Violet500
    val PurpleLight = RuleUpPalette.Violet400

    val Background = RuleUpPalette.Slate50
    val Surface = RuleUpPalette.White

    val TextPrimary = RuleUpPalette.Slate900
    val TextSecondary = RuleUpPalette.Slate500
    val TextMuted = RuleUpPalette.Slate400
    val TextSlate = RuleUpPalette.Slate600

    val Border = RuleUpPalette.Slate200
    val BorderLight = RuleUpPalette.Slate300

    val IndigoTint = RuleUpPalette.Indigo50
    val PurpleTint = RuleUpPalette.Violet100
    val GreenTint = RuleUpPalette.GreenTint
    val AmberTint = RuleUpPalette.AmberTint

    val Success = RuleUpPalette.Green500
    val SuccessText = RuleUpPalette.Green700
    val Danger = RuleUpPalette.Rose500

    val Kakao = RuleUpPalette.Kakao
    val KakaoText = RuleUpPalette.KakaoLabel
    val Naver = RuleUpPalette.Naver
    val Apple = RuleUpPalette.Apple
}

val DarkRuleUpColors =
    RuleUpColorScheme(
        brand = RuleUpPalette.Indigo500,
        brandStrong = RuleUpPalette.Violet400,
        brandAccent = RuleUpPalette.Violet400,
        brandSoft = Color(0xFF1E1B4B),
        background = Color(0xFF0B1120),
        surface = Color(0xFF111827),
        surfaceVariant = Color(0xFF1E293B),
        border = Color(0xFF1E293B),
        borderStrong = Color(0xFF334155),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = RuleUpPalette.Slate400,
        textMuted = RuleUpPalette.Slate500,
        textSlate = RuleUpPalette.Slate300,
        success = RuleUpPalette.Green500,
        onSuccess = Color(0xFF6EE7B7),
        successContainer = Color(0xFF064E3B),
        danger = RuleUpPalette.Rose500,
        warningContainer = Color(0xFF422006),
        isDark = true,
    )
