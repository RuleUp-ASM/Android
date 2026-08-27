package com.ruleup.designsystem.category

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.R
import com.ruleup.domain.entity.category.Category

/**
 * 카테고리별 강조색(Figma 카드 아이콘 톤).
 * `else` 를 두면 새 분류가 조용히 기본색으로 떨어진다 — 12종을 모두 적어 컴파일러가 잡게 둔다.
 */
fun categoryAccentColor(category: Category?): Color =
    when (category) {
        Category.WAKE_SLEEP, Category.MIND -> Color(0xFF6C5CE7)
        Category.READING, Category.STUDY -> Color(0xFF22C55E)
        Category.DIET_HEALTH, Category.HOUSEKEEPING -> Color(0xFFF59E0B)
        Category.EXERCISE -> Color(0xFFF43F5E)
        Category.FINANCE, Category.CAREER_PRODUCTIVITY -> Color(0xFF3B82F6)
        Category.HOBBY, Category.DETOX -> Color(0xFF06B6D4)
        // "기타"에 다른 분류와 같은 강조색을 주면 특정 분야처럼 보인다. 중립 회색으로 둔다.
        Category.ETC -> Color(0xFF94A3B8)
        null -> Color(0xFF6C5CE7)
    }

@DrawableRes
fun categoryIconRes(category: Category?): Int =
    when (category) {
        Category.EXERCISE -> R.drawable.ic_cat_exercise
        Category.WAKE_SLEEP -> R.drawable.ic_cat_wakeup
        Category.DIET_HEALTH -> R.drawable.ic_cat_health
        Category.STUDY, Category.READING -> R.drawable.ic_cat_reading
        Category.MIND -> R.drawable.ic_cat_meditation
        Category.FINANCE -> R.drawable.ic_cat_finance
        Category.HOBBY -> R.drawable.ic_cat_hobby
        Category.CAREER_PRODUCTIVITY -> R.drawable.ic_cat_work
        // 전용 에셋이 없어 중립 아이콘을 공유한다. TODO(#185): 에셋 확보 시 분리.
        Category.HOUSEKEEPING, Category.DETOX, Category.ETC -> R.drawable.ic_cat_etc
        null -> R.drawable.ic_cat_exercise
    }

fun categoryEmoji(category: Category): String =
    when (category) {
        Category.EXERCISE -> "🏃"
        Category.WAKE_SLEEP -> "🌅"
        Category.DIET_HEALTH -> "🥗"
        Category.STUDY -> "📖"
        Category.READING -> "📚"
        Category.MIND -> "🧘"
        Category.FINANCE -> "💰"
        Category.HOBBY -> "🎨"
        Category.HOUSEKEEPING -> "🧹"
        Category.CAREER_PRODUCTIVITY -> "💼"
        Category.DETOX -> "🌿"
        Category.ETC -> "✨"
    }
