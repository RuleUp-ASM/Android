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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ruleup.designsystem.category.categoryAccentColor
import com.ruleup.designsystem.category.categoryIconRes
import com.ruleup.designsystem.component.RuleUpSuspendedSheet
import com.ruleup.designsystem.component.RuleUpTopBar
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.NickNameUtil
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditEffect
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditIntent
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditState
import com.ruleup.profile.presentation.edit.viewmodel.ProfileEditViewModel
import com.ruleup.tti.presentation.TtiScreenEffect
import com.ruleup.tti.presentation.rememberTtiLargeContent
import com.ruleup.tti.presentation.ttiContentDrawn
import com.ruleup.ui.helper.LocalMessageHelper

private val AvatarGradient = listOf(RuleUpPalette.Primary600, RuleUpPalette.Primary300)

/** 프로필 편집. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileEditScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TtiScreenEffect(loading = state.isLoading)
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

    ProfileEditContent(
        state = state,
        onIntent = viewModel::onIntent,
        onPickImage = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        modifier = modifier,
    )
}

/** 화면 본문. */
@Composable
internal fun ProfileEditContent(
    state: ProfileEditState,
    onIntent: (ProfileEditIntent) -> Unit,
    onPickImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(RuleUpTheme.colors.background)
                .statusBarsPadding(),
    ) {
        // 저장 버튼을 숨기지 않고, 눌렀을 때 정지 사실을 말한다.
        state.saveBlock?.let { block ->
            RuleUpSuspendedSheet(
                title = "지금은 작성·수정할 수 없어요",
                description = "운영 정책 위반으로 닉네임·프로필·챌린지·방 공지 작성이 정지됐어요.",
                until = block.until,
                onConfirm = { onIntent(ProfileEditIntent.DismissSaveBlock) },
                onOpenHistory = { onIntent(ProfileEditIntent.OpenSanctionHistory) },
                onDismiss = { onIntent(ProfileEditIntent.DismissSaveBlock) },
            )
        }
        EditTopBar(
            isSaving = state.isSaving,
            onBack = { onIntent(ProfileEditIntent.Back) },
            onSave = { onIntent(ProfileEditIntent.Save) },
        )

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RuleUpTheme.colors.brand)
                }

            state.profile == null ->
                Column(
                    Modifier.fillMaxSize().ttiContentDrawn(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = state.errorMessage ?: "프로필을 불러오지 못했어요",
                        color = RuleUpTheme.colors.textSecondary,
                        style = RuleUpTheme.typography.labelMedium,
                    )
                    TextButton(onClick = { onIntent(ProfileEditIntent.Load) }) { Text("다시 시도") }
                }

            else ->
                EditBody(
                    state = state,
                    onIntent = onIntent,
                    onPickImage = onPickImage,
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
    RuleUpTopBar(
        title = "프로필 편집",
        onBack = onBack,
    ) {
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
                color = RuleUpPalette.BgSurface,
                style = RuleUpTheme.typography.bodyBold,
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
    if (state.profile == null) return
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 40.dp)
                .ttiContentDrawn(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 사진 — 누르면 갤러리, 1시 방향 X 로 제거
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(88.dp)) {
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                            .background(Brush.linearGradient(AvatarGradient))
                            .singleClickable(onClick = { if (!state.isImageBusy) onPickImage() }),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        state.isImageBusy ->
                            CircularProgressIndicator(color = RuleUpPalette.BgSurface, modifier = Modifier.size(26.dp))

                        state.imagePreviewUrl != null -> {
                            val onImageSettled = rememberTtiLargeContent()
                            AsyncImage(
                                model = state.imagePreviewUrl,
                                contentDescription = "프로필 이미지",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onSuccess = { onImageSettled() },
                                onError = { onImageSettled() },
                            )
                        }

                        else ->
                            Text(
                                text = state.nickname.take(1).ifBlank { "?" },
                                color = RuleUpPalette.BgSurface,
                                // 장식용 글리프라 타입 스케일(최대 22)에 넣으면 확 줄어든다.
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                            )
                    }
                }
                if (!state.isImageBusy && state.imagePreviewUrl != null) {
                    RemoveImageButton(
                        onClick = { onIntent(ProfileEditIntent.RemoveImage) },
                        // 지름 88 원의 1시 방향(중심 66, 6)에 26 버튼의 중심을 둔다.
                        modifier = Modifier.offset(x = 53.dp, y = (-7).dp),
                    )
                }
            }
        }

        // 닉네임
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "닉네임",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.smallBold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text =
                        if (state.nicknameLocked) {
                            "30일 1회 변경 가능 · 남은 ${state.nicknameLockedDays}일"
                        } else {
                            "30일 1회 변경 가능"
                        },
                    color = if (state.nicknameLocked) RuleUpPalette.StatusWarn else RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
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
                        RuleUpTheme.typography.section.copy(
                            color =
                                if (state.nicknameLocked) {
                                    RuleUpTheme.colors.textMuted
                                } else {
                                    RuleUpTheme.colors.textPrimary
                                },
                        ),
                    cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${state.nickname.length}/${NickNameUtil.MAX_LENGTH}",
                    color = RuleUpTheme.colors.textMuted,
                    style = RuleUpTheme.typography.caption,
                )
            }
        }

        // 관심 분야
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "관심 분야",
                    color = RuleUpTheme.colors.textSecondary,
                    style = RuleUpTheme.typography.smallBold,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${state.selectedCategories.size} / ${state.maxSelectable}",
                    color = RuleUpTheme.colors.brand,
                    style = RuleUpTheme.typography.captionBold,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Category.entries.forEach { category ->
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
            Text(text = "💡", style = RuleUpTheme.typography.body)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "관심 분야는 추천 챌린지와 알림에 사용돼요",
                color = RuleUpTheme.colors.brand,
                style = RuleUpTheme.typography.smallMedium,
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
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_close),
            contentDescription = "사진 제거",
            tint = RuleUpTheme.colors.textSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun CategoryChip(
    category: Category,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(categoryIconRes(category)),
                contentDescription = null,
                // 선택된 칩은 그라데이션 바탕이라 강조색 아이콘이 묻힌다.
                tint = if (selected) RuleUpPalette.BgSurface else categoryAccentColor(category),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = category.label,
                color = if (selected) RuleUpPalette.BgSurface else RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.smallBold,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun ProfileEditContentPreview() {
    RuleUpTheme {
        ProfileEditContent(
            state =
                com.ruleup.profile.presentation.edit.viewmodel.ProfileEditState.initial.copy(
                    isLoading = false,
                ),
            onIntent = {
            },
            onPickImage = { },
        )
    }
}
