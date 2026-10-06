package com.ruleup.designsystem.category

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.R
import com.ruleup.domain.entity.category.Category

/** 카테고리별 강조색(Figma 1550:2). 카테고리마다 다른 색이라 색만 보고도 분야를 가른다. */
fun categoryAccentColor(category: Category?): Color =
    when (category) {
        Category.EXERCISE -> Color(0xFFE11D48)
        Category.WAKE_SLEEP -> Color(0xFF4F46E5)
        Category.DIET_HEALTH -> Color(0xFF65A30D)
        Category.STUDY -> Color(0xFF2563EB)
        Category.READING -> Color(0xFF92400E)
        Category.MIND -> Color(0xFF9333EA)
        Category.FINANCE -> Color(0xFFCA8A04)
        Category.HOBBY -> Color(0xFFDB2777)
        Category.HOUSEKEEPING -> Color(0xFF0891B2)
        Category.CAREER_PRODUCTIVITY -> Color(0xFFEA580C)
        Category.DETOX -> Color(0xFF059669)
        // "기타"에 채도 있는 색을 주면 특정 분야처럼 보인다.
        Category.ETC, null -> Color(0xFF64748B)
    }

@DrawableRes
fun categoryIconRes(category: Category?): Int =
    when (category) {
        Category.EXERCISE -> R.drawable.ic_cat_exercise
        Category.WAKE_SLEEP -> R.drawable.ic_cat_wake_sleep
        Category.DIET_HEALTH -> R.drawable.ic_cat_diet_health
        Category.STUDY -> R.drawable.ic_cat_study
        Category.READING -> R.drawable.ic_cat_reading
        Category.MIND -> R.drawable.ic_cat_mind
        Category.FINANCE -> R.drawable.ic_cat_finance
        Category.HOBBY -> R.drawable.ic_cat_hobby
        Category.HOUSEKEEPING -> R.drawable.ic_cat_housekeeping
        Category.CAREER_PRODUCTIVITY -> R.drawable.ic_cat_career_productivity
        Category.DETOX -> R.drawable.ic_cat_detox
        Category.ETC, null -> R.drawable.ic_cat_etc
    }
