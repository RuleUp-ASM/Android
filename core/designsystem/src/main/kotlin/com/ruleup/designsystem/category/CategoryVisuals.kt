package com.ruleup.designsystem.category

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.R
import com.ruleup.domain.entity.category.Category

// 카테고리 → 표시용 색/아이콘 매핑 단일 소스.

/** 카테고리별 강조색(Figma 카드 아이콘 톤). */
fun categoryAccentColor(category: Category?): Color =
    when (category) {
        Category.WAKE_UP, Category.MEDITATION -> Color(0xFF6C5CE7)
        Category.READING, Category.STUDY, Category.WRITING -> Color(0xFF22C55E)
        Category.HEALTH, Category.COOKING -> Color(0xFFF59E0B)
        Category.EXERCISE -> Color(0xFFF43F5E)
        Category.FINANCE, Category.WORK -> Color(0xFF3B82F6)
        Category.MUSIC, Category.HOBBY -> Color(0xFF06B6D4)
        else -> Color(0xFF6C5CE7)
    }

/** 카테고리별 카드 아이콘. */
@DrawableRes
fun categoryIconRes(category: Category?): Int =
    when (category) {
        Category.EXERCISE -> R.drawable.ic_cat_exercise
        Category.READING, Category.STUDY -> R.drawable.ic_cat_reading
        Category.MEDITATION -> R.drawable.ic_cat_meditation
        Category.HEALTH -> R.drawable.ic_cat_health
        Category.WAKE_UP -> R.drawable.ic_cat_wakeup
        Category.WORK -> R.drawable.ic_cat_work
        Category.HOBBY -> R.drawable.ic_cat_hobby
        Category.COOKING -> R.drawable.ic_cat_cooking
        Category.FINANCE -> R.drawable.ic_cat_finance
        Category.ENVIRONMENT -> R.drawable.ic_cat_environment
        Category.RELATIONSHIP -> R.drawable.ic_cat_relationship
        Category.MUSIC -> R.drawable.ic_cat_music
        Category.WRITING -> R.drawable.ic_cat_writing
        Category.CODING -> R.drawable.ic_cat_coding
        null -> R.drawable.ic_cat_exercise
    }

/**
 * 카테고리별 표시 이모지.
 *
 * 예전엔 `Category` enum 이 직접 들고 있었는데, 색·아이콘과 갈라져 있을 이유가 없어 여기로 모았다.
 * (`label` 은 서버 응답 매칭 키를 겸해서 도메인에 남아 있다 — `Category` KDoc 참고.)
 */
fun categoryEmoji(category: Category): String =
    when (category) {
        Category.EXERCISE -> "🏃"
        Category.READING -> "📚"
        Category.MEDITATION -> "🧘"
        Category.HEALTH -> "💧"
        Category.WAKE_UP -> "🌅"
        Category.WORK -> "💼"
        Category.STUDY -> "📖"
        Category.HOBBY -> "🎨"
        Category.COOKING -> "🍳"
        Category.FINANCE -> "💰"
        Category.ENVIRONMENT -> "🌱"
        Category.RELATIONSHIP -> "🤝"
        Category.MUSIC -> "🎵"
        Category.WRITING -> "✍️"
        Category.CODING -> "💻"
    }
