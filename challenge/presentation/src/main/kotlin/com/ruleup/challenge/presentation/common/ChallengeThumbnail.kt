package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.ruleup.designsystem.category.CategoryCover
import com.ruleup.domain.entity.category.Category
import com.ruleup.tti.presentation.rememberTtiLargeContent

/**
 * 목록용 챌린지 썸네일. 대표 사진이 있으면 사진을, 없으면 상세와 같은 카테고리 기본 커버를 그린다.
 * 사진은 기본 커버 위에 덮어서, 불러오는 동안이나 못 불러왔을 때도 기본 커버가 보인다.
 */
@Composable
internal fun ChallengeThumbnail(
    imageUrl: String?,
    category: Category?,
    size: Dp,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius))) {
        CategoryCover(category = category, modifier = Modifier.fillMaxSize())
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
