package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.tti.presentation.rememberTtiLargeContent

/**
 * 챌린지 대표 사진. 마이페이지 프로필 사진처럼 사진을 누르면 갤러리, 모서리 X 로 뺀다.
 *
 * [image] 는 새로 고른 사진 uri 나 이미 등록된 사진 주소다. 없으면 빈 칸을 그린다.
 */
@Composable
internal fun CoverImagePicker(
    image: String?,
    enabled: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.surfaceVariant)
                    .singleClickable(enabled = enabled, onClick = onPick),
            contentAlignment = Alignment.Center,
        ) {
            if (image != null) {
                val onImageSettled = rememberTtiLargeContent()
                AsyncImage(
                    model = image,
                    contentDescription = "챌린지 사진. 누르면 다른 사진을 고른다",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onSuccess = { onImageSettled() },
                    onError = { onImageSettled() },
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gallery),
                        contentDescription = null,
                        tint = if (enabled) RuleUpTheme.colors.textSecondary else RuleUpTheme.colors.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = "사진 고르기",
                        color = if (enabled) RuleUpTheme.colors.textSecondary else RuleUpTheme.colors.textMuted,
                        style = RuleUpTheme.typography.smallMedium,
                    )
                }
            }
        }
        if (enabled && image != null) {
            RemoveImageButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

@Composable
private fun RemoveImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, CircleShape)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "사진 제거",
            tint = RuleUpTheme.colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun CoverImagePickerEmptyPreview() {
    RuleUpTheme {
        CoverImagePicker(image = null, enabled = true, onPick = {}, onRemove = {}, modifier = Modifier.padding(16.dp))
    }
}
