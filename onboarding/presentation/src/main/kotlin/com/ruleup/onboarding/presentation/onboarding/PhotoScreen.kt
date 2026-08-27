package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.onboarding.domain.navigation.OnboardingTermsPage
import com.ruleup.onboarding.domain.observability.OnboardingStep
import com.ruleup.onboarding.presentation.component.OnboardingScaffold
import com.ruleup.onboarding.presentation.onboarding.component.OnboardingFlowPreview
import com.ruleup.onboarding.presentation.onboarding.component.SectionHeader
import com.ruleup.onboarding.presentation.onboarding.component.rememberProfileImagePicker
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/**
 * 05 · 프로필 사진. 선택이라 고르지 않고 넘어가도 된다 — 건너뛰면 닉네임 첫 글자 아바타로 시작한다.
 *
 * 사진은 가입 요청에 실리지 않는다. 가입을 마치고 발급받은 accessToken 으로 따로 올린다.
 */
@Composable
fun PhotoContent(
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
    imageUri: String? = null,
) {
    val nav = LocalNavigationHelper.current
    val imagePicker =
        rememberProfileImagePicker { uri -> onIntent(OnboardingIntent.SetProfileIcon(uri)) }

    OnboardingScaffold(
        step = OnboardingStep.PHOTO,
        skipped = imageUri.isNullOrBlank(),
        buttonText = "다음",
        modifier = modifier,
        onNext = { nav.navigateTo(OnboardingTermsPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "프로필 사진을 등록할까요?",
            subtitle = "그룹 멤버들에게 보여요",
        )

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
                    Image(
                        painter = painterResource(com.ruleup.designsystem.R.drawable.ic_person),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(61.dp),
                    )
                } else {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().clip(RoundedCornerShape(70.dp)),
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(2.dp, RuleUpTheme.colors.brand, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(com.ruleup.designsystem.R.drawable.ic_edit),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(RuleUpTheme.colors.brand),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SourceCard(
                modifier = Modifier.weight(1f),
                iconBackground = RuleUpGradients.Brand,
                icon = com.ruleup.designsystem.R.drawable.ic_camera,
                title = "카메라로 촬영",
                caption = "바로 찍어 올리기",
                onClick = { imagePicker.launchCamera() },
            )
            SourceCard(
                modifier = Modifier.weight(1f),
                iconBackground = RuleUpGradients.Warm,
                icon = com.ruleup.designsystem.R.drawable.ic_gallery,
                title = "갤러리에서 선택",
                caption = "앨범에서 고르기",
                onClick = { imagePicker.launchGallery() },
            )
        }
    }
}

@Composable
private fun SourceCard(
    iconBackground: Brush,
    icon: Int,
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
                .singleClickable { onClick() },
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
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            title,
            modifier = Modifier.padding(top = RuleUpTheme.spacing.sm),
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.bodyBold,
        )
        Text(caption, color = RuleUpTheme.colors.textSecondary, style = RuleUpTheme.typography.tiny)
    }
}

@Preview
@Composable
private fun PhotoScreenPreview() {
    OnboardingFlowPreview { PhotoContent(onIntent = {}) }
}
