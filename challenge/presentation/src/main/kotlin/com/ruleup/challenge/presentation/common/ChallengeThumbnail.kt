package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import coil3.compose.AsyncImage
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.domain.entity.category.Category
import com.ruleup.tti.presentation.rememberTtiLargeContent

/** 챌린지 상세가 대표 사진 대신 쓰는 카테고리 타일(카테고리색 바탕 + 이모지). */
@Composable
internal fun CategoryTile(
    category: Category?,
    size: Dp,
    cornerRadius: Dp,
    emojiSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
                .background(categoryAccentColor(category)),
        contentAlignment = Alignment.Center,
    ) {
        // 장식용 글리프라 타입 스케일에 넣으면 확 줄어든다.
        Text(text = category?.let(::categoryEmoji) ?: "🎯", fontSize = emojiSize)
    }
}

/**
 * 목록용 챌린지 썸네일. 대표 사진이 있으면 사진을, 없으면 상세와 같은 [CategoryTile] 을 그린다.
 * 사진은 타일 위에 덮어서, 불러오는 동안이나 못 불러왔을 때도 타일이 보인다.
 */
@Composable
internal fun ChallengeThumbnail(
    imageUrl: String?,
    category: Category?,
    size: Dp,
    cornerRadius: Dp,
    emojiSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius))) {
        CategoryTile(category = category, size = size, cornerRadius = cornerRadius, emojiSize = emojiSize)
        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            val onImageSettled = rememberTtiLargeContent()
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { onImageSettled() },
                onError = { onImageSettled() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
