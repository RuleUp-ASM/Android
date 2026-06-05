package com.ruleup.onboarding.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/** 01 · 프로필 아이콘 (1/4). */
@Composable
fun ProfileIconContent(
    modifier: Modifier = Modifier,
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onCameraClick: () -> Unit = {},
    onGalleryClick: () -> Unit = {},
    imageUri: String? = null,
) {
    ProfileSetupScaffold(
        step = 0,
        buttonText = "다음",
        modifier = modifier,
        onNext = onNext,
        onBack = onBack,
    ) {
        SectionHeader(
            title = "프로필 아이콘을 골라주세요",
            subtitle = "사진을 올리거나 기본 아이콘을 사용할 수 있어요",
        )

        // 메인 아바타 + 카메라 뱃지
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(70.dp))
                        .background(RuleUpGradients.Brand)
                        .border(5.dp, Color.White, RoundedCornerShape(70.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri.isNullOrEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .size(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(2.dp, RuleUpTheme.colors.brand, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                    }
                } else {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(70.dp)),
                    )
                }
            }
        }

        // 카메라 / 갤러리 카드
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SourceCard(
                modifier = Modifier.weight(1f),
                iconBackground = RuleUpGradients.Brand,
                emoji = "📷",
                title = "카메라로 촬영",
                caption = "바로 찍어 올리기",
                onClick = {
                    onCameraClick()
                },
            )
            SourceCard(
                modifier = Modifier.weight(1f),
                iconBackground = RuleUpGradients.Warm,
                emoji = "🖼️",
                title = "갤러리에서 선택",
                caption = "앨범에서 고르기",
                onClick = {
                    onGalleryClick()
                },
            )
        }
    }
}

@Composable
private fun SourceCard(
    iconBackground: Brush,
    emoji: String,
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .height(96.dp)
                .clip(RuleUpTheme.shapes.card)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card)
                .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Text(
            title,
            modifier = Modifier.padding(top = RuleUpTheme.spacing.sm),
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(caption, color = RuleUpTheme.colors.textSecondary, fontSize = 10.sp)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ProfileIconScreenPreview() {
    RuleUpTheme { ProfileIconContent() }
}
