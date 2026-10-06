package com.ruleup.designsystem.category

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.ruleup.designsystem.R
import com.ruleup.domain.entity.category.Category

/**
 * 사진 없는 챌린지의 기본 커버(Figma 1550:2). 기상·수면은 아이콘 패턴 + 배지, 나머지는 카테고리 장면 그림이다.
 *
 * 그림보다 세로로 긴 자리에서는 그림을 위에 붙이고 남는 아래를 배경이 채운다 — 꽉 채워 자르면 양옆이 잘려 장면이 안 보인다.
 */
@Composable
fun CategoryCover(
    category: Category?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.background(coverBackground(category))) {
        val tall = maxHeight > maxWidth * ART_ASPECT
        when (val art = coverArt(category)) {
            is CoverArt.Scene ->
                Image(
                    painter = painterResource(art.res),
                    contentDescription = null,
                    alignment = if (tall) Alignment.TopCenter else Alignment.Center,
                    contentScale = if (tall) ContentScale.FillWidth else ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            CoverArt.IconPattern -> IconPatternCover(category = category, tall = tall, modifier = Modifier.fillMaxSize())
        }
    }
}

/** 목록 행의 카테고리 아이콘 타일. 연한 강조색 바탕에 강조색 아이콘. */
@Composable
fun CategoryIconTile(
    category: Category?,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val accent = categoryAccentColor(category)
    Box(
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.3f))
                .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(categoryIconRes(category)),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

private sealed interface CoverArt {
    data class Scene(
        @DrawableRes val res: Int,
    ) : CoverArt

    data object IconPattern : CoverArt
}

private fun coverArt(category: Category?): CoverArt =
    when (category) {
        Category.WAKE_SLEEP -> CoverArt.IconPattern
        Category.EXERCISE -> CoverArt.Scene(R.drawable.cover_cat_exercise)
        Category.DIET_HEALTH -> CoverArt.Scene(R.drawable.cover_cat_diet_health)
        Category.STUDY -> CoverArt.Scene(R.drawable.cover_cat_study)
        Category.READING -> CoverArt.Scene(R.drawable.cover_cat_reading)
        Category.MIND -> CoverArt.Scene(R.drawable.cover_cat_mind)
        Category.FINANCE -> CoverArt.Scene(R.drawable.cover_cat_finance)
        Category.HOBBY -> CoverArt.Scene(R.drawable.cover_cat_hobby)
        Category.HOUSEKEEPING -> CoverArt.Scene(R.drawable.cover_cat_housekeeping)
        Category.CAREER_PRODUCTIVITY -> CoverArt.Scene(R.drawable.cover_cat_career_productivity)
        Category.DETOX -> CoverArt.Scene(R.drawable.cover_cat_detox)
        Category.ETC, null -> CoverArt.Scene(R.drawable.cover_cat_etc)
    }

private fun coverBackground(category: Category?): Brush =
    when (category) {
        // 밤에서 새벽으로 넘어가는 하늘.
        Category.WAKE_SLEEP ->
            Brush.verticalGradient(
                0f to Color(0xFF1E1B4B),
                0.6f to Color(0xFF4F46E5),
                1f to Color(0xFFFB923C),
            )
        else -> {
            val accent = categoryAccentColor(category)
            Brush.linearGradient(listOf(lerp(accent, Color.White, 0.18f), lerp(accent, Color.Black, 0.28f)))
        }
    }

@Composable
private fun IconPatternCover(
    category: Category?,
    tall: Boolean,
    modifier: Modifier = Modifier,
) {
    val icon = painterResource(categoryIconRes(category))
    val accent = categoryAccentColor(category)
    Canvas(modifier = modifier) {
        val step = if (tall) size.width * PATTERN_STEP_RATIO else size.minDimension / 3
        val tileTint = ColorFilter.tint(Color.White.copy(alpha = 0.16f))
        var row = 0
        var y = -step / 4
        while (y < size.height) {
            var x = if (row % 2 == 1) step / 4 else -step / 4
            while (x < size.width) {
                drawIcon(icon, Offset(x, y), step / 2, tileTint)
                x += step
            }
            y += step
            row++
        }

        val center = Offset(size.width / 2, if (tall) size.width * BADGE_CENTER_RATIO else size.height / 2)
        val radius = if (tall) size.width * BADGE_RADIUS_RATIO else size.minDimension * 0.32f
        drawCircle(color = Color.White, radius = radius, center = center)
        val iconSize = radius * 1.38f
        drawIcon(icon, center - Offset(iconSize / 2, iconSize / 2), iconSize, ColorFilter.tint(accent))
    }
}

private fun DrawScope.drawIcon(
    icon: Painter,
    topLeft: Offset,
    side: Float,
    tint: ColorFilter,
) {
    translate(topLeft.x, topLeft.y) {
        with(icon) { draw(size = Size(side, side), colorFilter = tint) }
    }
}

// 장면 그림의 세로/가로 비(240×300).
private const val ART_ASPECT = 300f / 240f

// 시안 B 의 240 폭 기준 값: 무늬 간격 56 · 배지 중심 y 130 · 배지 반지름 52.
private const val PATTERN_STEP_RATIO = 56f / 240f
private const val BADGE_CENTER_RATIO = 130f / 240f
private const val BADGE_RADIUS_RATIO = 52f / 240f
