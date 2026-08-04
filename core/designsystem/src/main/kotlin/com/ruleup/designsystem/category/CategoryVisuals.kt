package com.ruleup.designsystem.category

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.ruleup.designsystem.R
import com.ruleup.domain.entity.category.Category

// 카테고리 → 표시용 색/아이콘 매핑 단일 소스.

/**
 * 카테고리별 강조색(Figma 카드 아이콘 톤).
 *
 * `else` 를 두지 않고 12종을 모두 적는다. 예전엔 `else -> 보라색` 이라 분류가 바뀌어도 컴파일이
 * 조용히 통과하고 새 분류가 기본색으로 떨어졌다 — 카테고리 개편에서 가장 놓치기 쉬운 자리라
 * 컴파일러가 잡게 둔다. 기본색은 null 에만 준다.
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

/**
 * 카테고리별 카드 아이콘.
 *
 * 정리·살림·절제·디톡스·기타는 전용 에셋이 없어 중립 아이콘을 공유한다. 새 디자인의 탐색
 * 카테고리 그리드와 온보딩 관심사 칩이 텍스트 전용이라 아이콘이 남는 곳은 홈·탐색 카드뿐이고,
 * 3종을 새로 그릴지는 그 화면들을 확정한 뒤 판단한다.
 */
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
        // TODO(#185): 전용 에셋을 확보하면 분리한다.
        Category.HOUSEKEEPING, Category.DETOX, Category.ETC -> R.drawable.ic_cat_etc
        null -> R.drawable.ic_cat_exercise
    }

/**
 * 카테고리별 표시 이모지.
 *
 * 예전엔 `Category` enum 이 직접 들고 있었는데, 색·아이콘과 갈라져 있을 이유가 없어 여기로 모았다.
 */
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
