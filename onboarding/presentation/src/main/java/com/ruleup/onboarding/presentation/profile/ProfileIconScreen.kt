package com.ruleup.onboarding.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.ruleup.onboarding.domain.ProfileNicknamePage
import com.ruleup.onboarding.presentation.profile.component.ProfileFlowPreview
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.ui.component.ProfileSetupScaffold
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme
import java.io.File

/** 01 · 프로필 아이콘 (1/4). 갤러리/카메라 런처를 직접 띄우고 결과를 [ProfileIntent.SetProfileIcon] 으로 보낸다. */
@Composable
fun ProfileIconContent(
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    imageUri: String? = null,
) {
    val nav = LocalNavigationHelper.current
    val context = LocalContext.current

    val gallery =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onIntent(ProfileIntent.SetProfileIcon(uri.toString()))
        }

    // 촬영 결과가 저장될 URI. 카메라 앱이 떠 있는 동안 프로세스가 재생성될 수 있어 saveable 로 보존한다.
    var cameraImageUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            val uri = cameraImageUri
            if (success && uri != null) onIntent(ProfileIntent.SetProfileIcon(uri))
        }

    ProfileSetupScaffold(
        step = 0,
        buttonText = "다음",
        modifier = modifier,
        onNext = { nav.navigateTo(ProfileNicknamePage) },
        onBack = { nav.navigateToBack() },
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
                    val uri = createCameraImageUri(context)
                    cameraImageUri = uri.toString()
                    camera.launch(uri)
                },
            )
            SourceCard(
                modifier = Modifier.weight(1f),
                iconBackground = RuleUpGradients.Warm,
                emoji = "🖼️",
                title = "갤러리에서 선택",
                caption = "앨범에서 고르기",
                onClick = {
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            )
        }
    }
}

/** 카메라 촬영 결과를 받을 캐시 파일을 만들고 FileProvider URI 로 변환한다. */
private fun createCameraImageUri(context: Context): Uri {
    val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("profile_", ".jpg", cameraDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
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
    ProfileFlowPreview { ProfileIconContent(onIntent = {}) }
}
