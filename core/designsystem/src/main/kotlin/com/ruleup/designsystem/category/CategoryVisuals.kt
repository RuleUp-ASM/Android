package com.ruleup.designsystem.category

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.R
import com.ruleup.domain.entity.user.InterestCategory

// 카테고리 → 표시용 색/아이콘 매핑 단일 소스.

/** 카테고리별 강조색(Figma 카드 아이콘 톤). */
fun categoryAccentColor(category: InterestCategory?): Color =
    when (category) {
        InterestCategory.WAKE_UP, InterestCategory.MEDITATION -> Color(0xFF6C5CE7)
        InterestCategory.READING, InterestCategory.STUDY, InterestCategory.WRITING -> Color(0xFF22C55E)
        InterestCategory.HEALTH, InterestCategory.COOKING -> Color(0xFFF59E0B)
        InterestCategory.EXERCISE -> Color(0xFFF43F5E)
        InterestCategory.FINANCE, InterestCategory.WORK -> Color(0xFF3B82F6)
        InterestCategory.MUSIC, InterestCategory.HOBBY -> Color(0xFF06B6D4)
        else -> Color(0xFF6C5CE7)
    }

/** 카테고리별 카드 아이콘. */
@DrawableRes
fun categoryIconRes(category: InterestCategory?): Int =
    when (category) {
        InterestCategory.EXERCISE -> R.drawable.ic_cat_exercise
        InterestCategory.READING, InterestCategory.STUDY -> R.drawable.ic_cat_reading
        InterestCategory.MEDITATION -> R.drawable.ic_cat_meditation
        InterestCategory.HEALTH -> R.drawable.ic_cat_health
        InterestCategory.WAKE_UP -> R.drawable.ic_cat_wakeup
        InterestCategory.WORK -> R.drawable.ic_cat_work
        InterestCategory.HOBBY -> R.drawable.ic_cat_hobby
        InterestCategory.COOKING -> R.drawable.ic_cat_cooking
        InterestCategory.FINANCE -> R.drawable.ic_cat_finance
        InterestCategory.ENVIRONMENT -> R.drawable.ic_cat_environment
        InterestCategory.RELATIONSHIP -> R.drawable.ic_cat_relationship
        InterestCategory.MUSIC -> R.drawable.ic_cat_music
        InterestCategory.WRITING -> R.drawable.ic_cat_writing
        InterestCategory.CODING -> R.drawable.ic_cat_coding
        null -> R.drawable.ic_cat_exercise
    }
