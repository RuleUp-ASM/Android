package com.ruleup.challenge.presentation.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * 챌린지 대표 사진을 [content] 뒤에 깐다. 사진 위에 [scrim] 을 덮어 글자색을 바꾸지 않고도 읽히게 한다.
 * 사진이 없으면 [content] 만 그린다.
 */
@Composable
internal fun ChallengeCoverBackground(
    imageUrl: String?,
    scrim: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier) {
        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(scrim.copy(alpha = COVER_SCRIM_ALPHA)),
            )
        }
        content()
    }
}

private const val COVER_SCRIM_ALPHA = 0.72f
