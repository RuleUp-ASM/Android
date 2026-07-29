package com.ruleup.profile.presentation.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.InterestCategory
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditEffect
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditIntent
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditState
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditViewModel
import com.ruleup.ui.helper.LocalMessageHelper

private val AvatarGradient = listOf(RuleUpPalette.Indigo500, RuleUpPalette.Violet500)

/**
 * 프로필 편집 (피그마 434:566). 마이 홈 프로필 영역으로 진입한다.
 * 닉네임(2~12자·30일 제한·저장 시 선검사), 관심 분야(1~[maxSelectable]), 사진(갤러리/제거 — 즉시 반영).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageHelper = LocalMessageHelper.current
    val imagePicker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onIntent(ProfileEditIntent.PickImage(it.toString())) }
        }

    LaunchedEffect(Unit) {
        viewModel.onIntent(ProfileEditIntent.Load)
    }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileEditEffect.ShowMessage -> messageHelper.showToast(effect.message)
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        EditTopBar(
            isSaving = state.isSaving,
            onBack = { viewModel.onIntent(ProfileEditIntent.Back) },
            onSave = { viewModel.onIntent(ProfileEditIntent.Save) },
        )

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.profile == null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "프로필을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        fontSize = 14.sp,
                    )
                }

            else ->
                EditBody(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onPickImage = {
                        imagePicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                )
        }
    }
}

@Composable
private fun EditTopBar(
    isSaving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = "프로필 편집",
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(RuleUpTheme.colors.brand)
                    .singleClickable(onClick = onSave)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
        ) {
            Text(
                text = if (isSaving) "저장 중…" else "저장",
                color = RuleUpPalette.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(8.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditBody(
    state: ProfileEditState,
    onIntent: (ProfileEditIntent) -> Unit,
    onPickImage: () -> Unit,
) {
    val profile = state.profile ?: return
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ---------- 사진 ----------
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(16.dp))
                    .padding(vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(AvatarGradient)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    state.isImageBusy ->
                        CircularProgressIndicator(color = RuleUpPalette.White, modifier = Modifier.size(26.dp))

                    profile.profileImageUrl != null ->
                        AsyncImage(
                            model = profile.profileImageUrl,
                            contentDescription = "프로필 이미지",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )

                    else ->
                        Text(
                            text = state.nickname.take(1).ifBlank { "?" },
                            color = RuleUpPalette.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImageActionChip(label = "🖼 갤러리", enabled = !state.isImageBusy, onClick = onPickImage)
                ImageActionChip(
                    label = "🗑 제거",
                    enabled = !state.isImageBusy && profile.profileImageUrl != null,
                    onClick = { onIntent(ProfileEditIntent.RemoveImage) },
                )
            }
        }

        // ---------- 닉네임 ----------
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "닉네임",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text =
                        if (state.nicknameLocked) {
                            "30일 1회 변경 가능 · 남은 ${state.nicknameLockedDays}일"
                        } else {
                            "30일 1회 변경 가능"
                        },
                    color = if (state.nicknameLocked) RuleUpPalette.Amber500 else RuleUpTheme.colors.textMuted,
                    fontSize = 11.sp,
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(RuleUpTheme.colors.surface)
                        .border(
                            1.5.dp,
                            if (state.nicknameLocked) RuleUpTheme.colors.border else RuleUpTheme.colors.brand,
                            RoundedCornerShape(12.dp),
                        ).padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = state.nickname,
                    onValueChange = { onIntent(ProfileEditIntent.ChangeNickname(it)) },
                    singleLine = true,
                    enabled = !state.nicknameLocked,
                    textStyle =
                        TextStyle(
                            color =
                                if (state.nicknameLocked) {
                                    RuleUpTheme.colors.textMuted
                                } else {
                                    RuleUpTheme.colors.textPrimary
                                },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${state.nickname.length}/${ProfileEditState.NICKNAME_MAX_LENGTH}",
                    color = RuleUpTheme.colors.textMuted,
                    fontSize = 11.sp,
                )
            }
        }

        // ---------- 관심 분야 ----------
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "관심 분야",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.selectedCategories.size} / ${state.maxSelectable}",
                    color = RuleUpTheme.colors.brand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                InterestCategory.entries.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = category in state.selectedCategories,
                        onClick = { onIntent(ProfileEditIntent.ToggleCategory(category)) },
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RuleUpTheme.colors.brandSoft)
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "💡", fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "관심 분야는 추천 챌린지와 알림에 사용돼요",
                color = RuleUpTheme.colors.brand,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ImageActionChip(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surfaceVariant)
                .singleClickable(onClick = { if (enabled) onClick() })
                .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (enabled) RuleUpTheme.colors.brand else RuleUpTheme.colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CategoryChip(
    category: InterestCategory,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(19.dp))
                .then(
                    if (selected) {
                        Modifier.background(Brush.linearGradient(AvatarGradient))
                    } else {
                        Modifier
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(19.dp))
                    },
                ).singleClickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            text = "${category.emoji} ${category.label}",
            color = if (selected) RuleUpPalette.White else RuleUpTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
