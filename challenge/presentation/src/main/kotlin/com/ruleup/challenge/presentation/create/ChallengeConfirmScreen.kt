package com.ruleup.challenge.presentation.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.ruleup.challenge.domain.entity.ParamKind
import com.ruleup.challenge.domain.entity.ParamSpec
import com.ruleup.challenge.domain.entity.ParamValue
import com.ruleup.challenge.domain.entity.ParticipationType
import com.ruleup.challenge.domain.entity.RepeatDay
import com.ruleup.challenge.domain.entity.SelectedMethod
import com.ruleup.challenge.domain.entity.SignalSource
import com.ruleup.challenge.domain.entity.VerificationOption
import com.ruleup.challenge.domain.entity.VerificationType
import com.ruleup.challenge.domain.entity.WearableRequirement
import com.ruleup.challenge.presentation.create.component.ChallengeFlowPreview
import com.ruleup.challenge.presentation.create.component.CreateChallengeTopBar
import com.ruleup.challenge.presentation.create.component.DurationPickerSheet
import com.ruleup.challenge.presentation.create.component.GradientSwitch
import com.ruleup.challenge.presentation.create.component.InfoNote
import com.ruleup.challenge.presentation.create.component.ParamsEditor
import com.ruleup.challenge.presentation.create.component.SectionLabel
import com.ruleup.challenge.presentation.create.component.SmallBadge
import com.ruleup.challenge.presentation.create.component.rememberChallengeImagePicker
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeIntent
import com.ruleup.challenge.presentation.create.viewmodel.CreateChallengeState
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpPalette
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper
import kotlin.math.roundToInt

/** 02 · AI 추천 확인. 추천값을 항목별로 보여주고 자유롭게 수정한 뒤 확정한다. */
@Composable
fun ChallengeConfirmContent(
    onIntent: (CreateChallengeIntent) -> Unit,
    modifier: Modifier = Modifier,
    state: CreateChallengeState = CreateChallengeState.initial,
) {
    val nav = LocalNavigationHelper.current
    var showDurationSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(RuleUpTheme.colors.background)) {
        CreateChallengeTopBar(
            title = "AI 추천 확인",
            onBack = { nav.navigateToBack() },
            trailingText = "취소",
            onTrailingClick = { nav.navigateToBack() },
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { AiDoneBanner() }
            item { ChallengeInfoCard(title = state.title, description = state.description) }
            item { CoverPhotoSection(coverImageUri = state.coverImageUri, onIntent = onIntent) }
            item { CategorySection(state = state) }
            item { ParticipationSection(selected = state.participationType, onIntent = onIntent) }
            if (state.participationType == ParticipationType.GROUP) {
                item {
                    MaxParticipantsSection(
                        count = state.maxParticipants,
                        onIntent = onIntent,
                    )
                }
                item {
                    MannerTemperatureSection(
                        temperature = state.minMannerTemperature,
                        maxTemperature = state.maxMannerTemperature,
                        onIntent = onIntent,
                    )
                }
            }
            item {
                FrequencyAndPeriodSection(
                    state = state,
                    onIntent = onIntent,
                    onPeriodClick = { showDurationSheet = true },
                )
            }
            item { MethodSelector(state = state, onIntent = onIntent) }
            state.selectedOption?.let { option ->
                item { VerificationSnapshotCard(option = option, rationale = state.rationale) }
            }
            if (state.params.isNotEmpty()) {
                item { ParamsEditor(params = state.params, onIntent = onIntent) }
            }
            item { PenaltySection(state = state, onIntent = onIntent) }
            item {
                InfoNote(
                    emoji = "✨",
                    text = "확정 후에도 챌린지 시작 전에는 수정할 수 있어요",
                    background = RuleUpPalette.Violet100,
                    textColor = RuleUpTheme.colors.textSlate,
                )
            }
        }

        ConfirmBottomBar(
            isRecommending = state.isRecommending,
            isCreating = state.isCreating,
            onIntent = onIntent,
        )
    }

    if (showDurationSheet) {
        DurationPickerSheet(
            startDate = state.startDate,
            durationDays = state.durationDays,
            onConfirm = { startDate, durationDays ->
                onIntent(CreateChallengeIntent.SetPeriod(startDate, durationDays))
                showDurationSheet = false
            },
            onDismiss = { showDurationSheet = false },
        )
    }
}

@Composable
private fun AiDoneBanner() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.large)
                .background(Brush.linearGradient(listOf(RuleUpPalette.Indigo50, RuleUpPalette.Violet100)))
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(RuleUpGradients.Brand),
            contentAlignment = Alignment.Center,
        ) {
            Text("🤖", fontSize = 18.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "AI가 추천을 완료했어요",
                color = RuleUpTheme.colors.brandStrong,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "각 항목은 자유롭게 수정할 수 있어요",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun ChallengeInfoCard(
    title: String,
    description: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.card)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card)
                .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📝", fontSize = 14.sp)
            Text(
                "챌린지 정보",
                color = RuleUpTheme.colors.textMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.66.sp,
            )
        }
        Text(
            title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        if (description.isNotBlank()) {
            Text(
                description,
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun CoverPhotoSection(
    coverImageUri: String?,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    val imagePicker =
        rememberChallengeImagePicker { uri -> onIntent(CreateChallengeIntent.SetCoverImage(uri)) }
    val openGallery = { imagePicker.launchGallery() }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "배경 사진") {
            SmallBadge(
                text = "선택",
                background = RuleUpTheme.colors.surfaceVariant,
                textColor = RuleUpTheme.colors.textSecondary,
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RuleUpTheme.shapes.large),
        ) {
            if (coverImageUri != null) {
                AsyncImage(
                    model = coverImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(RuleUpPalette.Amber500, RuleUpPalette.Rose500, RuleUpPalette.Violet500),
                                ),
                            ),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .border(2.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("📷", fontSize = 24.sp)
                    }
                    Text(
                        "AI가 자동 선택한 배경",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.66.sp,
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .height(30.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .singleClickable(onClick = openGallery)
                        .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✏️", fontSize = 11.sp)
                Text("변경", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CoverSourceChip(emoji = "📷", label = "카메라", tinted = true) {
                imagePicker.launchCamera()
            }
            CoverSourceChip(emoji = "🖼️", label = "갤러리", onClick = openGallery)
            CoverSourceChip(emoji = "✨", label = "AI 추천") {
                onIntent(CreateChallengeIntent.SetCoverImage(null))
            }
        }
    }
}

@Composable
private fun CoverSourceChip(
    emoji: String,
    label: String,
    tinted: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .let { base ->
                    if (tinted) {
                        base.background(RuleUpTheme.colors.brandSoft)
                    } else {
                        base
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(18.dp))
                    }
                }.singleClickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(
            label,
            color = if (tinted) RuleUpTheme.colors.brandStrong else RuleUpTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CategorySection(state: CreateChallengeState) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "카테고리")
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RuleUpTheme.shapes.small)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFED7AA))),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.category?.let(::categoryEmoji) ?: "❓", fontSize = 22.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    state.category?.label ?: "분류 실패",
                    color = RuleUpTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (state.category != null) "제목에서 자동 분류했어요" else "제목을 수정해 다시 추천받아 주세요",
                    color = RuleUpTheme.colors.textSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun ParticipationSection(
    selected: ParticipationType,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "참여 방식")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ParticipationCard(
                modifier = Modifier.weight(1f),
                emoji = "🌱",
                title = "솔로",
                caption = "나와의 약속",
                background = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFED7AA))),
                selected = selected == ParticipationType.SOLO,
                onClick = { onIntent(CreateChallengeIntent.SetParticipationType(ParticipationType.SOLO)) },
            )
            ParticipationCard(
                modifier = Modifier.weight(1f),
                emoji = "👥",
                title = "그룹",
                caption = "함께 도전",
                background = Brush.linearGradient(listOf(RuleUpPalette.Indigo50, RuleUpPalette.Violet100)),
                selected = selected == ParticipationType.GROUP,
                onClick = { onIntent(CreateChallengeIntent.SetParticipationType(ParticipationType.GROUP)) },
            )
        }
    }
}

@Composable
private fun ParticipationCard(
    emoji: String,
    title: String,
    caption: String,
    background: Brush,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .height(108.dp)
                .clip(RuleUpTheme.shapes.large)
                .background(background)
                .let { base ->
                    if (selected) base.border(3.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.large) else base
                }.singleClickable(onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (selected) {
            SmallBadge(text = "AI 선택", background = RuleUpTheme.colors.brand, textColor = Color.White)
        }
        Text(emoji, fontSize = 28.sp)
        Text(title, color = RuleUpTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(caption, color = RuleUpTheme.colors.textSecondary, fontSize = 10.sp)
    }
}

/** 매너 온도 구간 라벨. 탭하면 대표값으로 설정한다. */
private data class MannerLevel(
    val label: String,
    val range: IntRange,
    val representative: Int,
    val background: Color,
    val textColor: Color,
)

private val mannerLevels =
    listOf(
        MannerLevel("매우 낮음", 37..49, 40, RuleUpPalette.Rose500.copy(alpha = 0.15f), RuleUpPalette.Rose500),
        MannerLevel("보통", 50..64, 55, RuleUpPalette.Amber500.copy(alpha = 0.15f), RuleUpPalette.Amber500),
        MannerLevel("높음", 65..79, 65, RuleUpPalette.Indigo500.copy(alpha = 0.15f), RuleUpPalette.Indigo500),
        MannerLevel("최고", 80..99, 85, RuleUpPalette.Green500.copy(alpha = 0.15f), RuleUpPalette.Green500),
    )

/** 그룹 전용 최대 참여 인원 스텝퍼(− n +). 범위는 [CreateChallengeState] 상수로 클램프한다. */
@Composable
private fun MaxParticipantsSection(
    count: Int,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "최대 참여 인원") {
            SmallBadge(
                text = "그룹 전용",
                background = RuleUpTheme.colors.brandSoft,
                textColor = RuleUpTheme.colors.brandStrong,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "방장 포함 인원",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StepperButton(symbol = "−", enabled = count > CreateChallengeState.GROUP_PARTICIPANTS_MIN) {
                    onIntent(CreateChallengeIntent.SetMaxParticipants(count - 1))
                }
                Text(
                    "$count",
                    color = RuleUpTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                StepperButton(symbol = "+", enabled = count < CreateChallengeState.GROUP_PARTICIPANTS_MAX) {
                    onIntent(CreateChallengeIntent.SetMaxParticipants(count + 1))
                }
            }
        }
    }
}

@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (enabled) RuleUpTheme.colors.brandSoft else RuleUpTheme.colors.surfaceVariant)
                .then(if (enabled) Modifier.singleClickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            color = if (enabled) RuleUpTheme.colors.brandStrong else RuleUpTheme.colors.textMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MannerTemperatureSection(
    temperature: Int,
    maxTemperature: Int,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    // 드래그 중에는 로컬 값만 갱신하고 손을 뗄 때 한 번만 커밋한다.
    var drag by remember(temperature) { mutableStateOf(temperature.toFloat()) }
    val shown = drag.roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "참여 매너 온도") {
            Row(
                modifier =
                    Modifier
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(RuleUpTheme.colors.brandSoft)
                        .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("👥", fontSize = 9.sp)
                Text(
                    "그룹 전용",
                    color = RuleUpTheme.colors.brandStrong,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.36.sp,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "최소 참여 기준",
                        color = RuleUpTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "내 매너 온도가 기준 이상이면 통과",
                        color = RuleUpTheme.colors.textMuted,
                        fontSize = 10.sp,
                    )
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    // 최저값은 "제한 없음"(기준 미설정)을 의미한다 — 생성 시 서버로 null 이 전송된다.
                    val noLimit = shown <= CreateChallengeState.MANNER_MIN
                    Text(
                        if (noLimit) "제한 없음" else "$shown",
                        style =
                            TextStyle(
                                brush =
                                    Brush.horizontalGradient(
                                        listOf(RuleUpPalette.Indigo500, RuleUpPalette.Violet500),
                                    ),
                                fontSize = if (noLimit) 20.sp else 30.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                    )
                    if (!noLimit) {
                        Text(
                            "℃",
                            color = RuleUpTheme.colors.brand,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Slider(
                    value = drag,
                    onValueChange = { drag = it },
                    onValueChangeFinished = {
                        onIntent(CreateChallengeIntent.SetMinMannerTemperature(shown))
                    },
                    valueRange =
                        CreateChallengeState.MANNER_MIN.toFloat()..maxTemperature.toFloat(),
                    colors =
                        SliderDefaults.colors(
                            thumbColor = RuleUpTheme.colors.brand,
                            activeTrackColor = RuleUpTheme.colors.brand,
                            inactiveTrackColor = RuleUpTheme.colors.surfaceVariant,
                        ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${CreateChallengeState.MANNER_MIN}℃ (최저)",
                        color = RuleUpTheme.colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "$maxTemperature℃ (최고)",
                        color = RuleUpTheme.colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RuleUpTheme.colors.surfaceVariant)
                        .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                mannerLevels.forEach { level ->
                    val active = shown in level.range
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) RuleUpTheme.colors.brand else level.background)
                                .singleClickable {
                                    onIntent(CreateChallengeIntent.SetMinMannerTemperature(level.representative))
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            level.label,
                            color = if (active) Color.White else level.textColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            InfoNote(
                emoji = "💡",
                text = "기준이 높을수록 신뢰도 있는 멤버만 참여해요",
                background = RuleUpTheme.colors.warningContainer,
                textColor = Color(0xFFB45309),
            )
        }
    }
}

@Composable
private fun FrequencyAndPeriodSection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
    onPeriodClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "빈도와 기간")
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔁", fontSize = 13.sp)
                    Text(
                        "반복 요일",
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "${state.repeatDays.size} / 7일",
                    color = RuleUpTheme.colors.brand,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RepeatDay.entries.forEach { day ->
                    RepeatDayChip(
                        day = day,
                        selected = day in state.repeatDays,
                        onClick = { onIntent(CreateChallengeIntent.ToggleRepeatDay(day)) },
                    )
                }
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(RuleUpTheme.colors.border),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📅", fontSize = 13.sp)
                    Text(
                        "기간",
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Row(
                    modifier =
                        Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(RuleUpTheme.colors.brandSoft)
                            .border(1.dp, RuleUpTheme.colors.brand, RoundedCornerShape(16.dp))
                            .singleClickable(onClick = onPeriodClick)
                            .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        ChallengeDates.durationLabel(state.durationDays),
                        color = RuleUpTheme.colors.brandStrong,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("·", color = RuleUpTheme.colors.brand, fontSize = 11.sp)
                    Text(
                        "${state.durationDays}일",
                        color = RuleUpTheme.colors.brandStrong,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepeatDayChip(
    day: RepeatDay,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .then(
                    if (selected) {
                        Modifier.background(RuleUpTheme.colors.brand)
                    } else {
                        Modifier
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(19.dp))
                    },
                ).singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.label,
            color = if (selected) Color.White else RuleUpTheme.colors.textMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** AUTO/MANUAL 인증 방식 선택. AUTO 옵션이 없는 루틴이면 자동 인증 카드는 비활성. */
@Composable
private fun MethodSelector(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "인증 방식")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MethodCard(
                modifier = Modifier.weight(1f),
                emoji = "⚡",
                title = "자동 인증",
                caption = "센서·권한으로 자동 확인",
                selected = state.selectedMethod == SelectedMethod.AUTO,
                enabled = state.hasAutoOption,
                onClick = { onIntent(CreateChallengeIntent.SelectMethod(SelectedMethod.AUTO)) },
            )
            MethodCard(
                modifier = Modifier.weight(1f),
                emoji = "✍️",
                title = "수동 인증",
                caption = "직접 체크",
                selected = state.selectedMethod == SelectedMethod.MANUAL,
                enabled = true,
                onClick = { onIntent(CreateChallengeIntent.SelectMethod(SelectedMethod.MANUAL)) },
            )
        }
        if (!state.hasAutoOption) {
            InfoNote(
                emoji = "ℹ️",
                text = "이 루틴은 자동 인증을 지원하지 않아요",
                background = RuleUpTheme.colors.surfaceVariant,
                textColor = RuleUpTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun MethodCard(
    emoji: String,
    title: String,
    caption: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Column(
        modifier =
            modifier
                .height(96.dp)
                .clip(RuleUpTheme.shapes.large)
                .background(if (enabled) RuleUpTheme.colors.surface else RuleUpTheme.colors.surfaceVariant)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.large)
                    } else {
                        Modifier.border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    },
                ).singleClickable(enabled = enabled, onClick = onClick)
                .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 24.sp)
        Text(
            title,
            color = if (enabled) RuleUpTheme.colors.textPrimary else RuleUpTheme.colors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(caption, color = RuleUpTheme.colors.textSecondary, fontSize = 9.sp)
    }
}

private fun VerificationType.label(): String =
    when (this) {
        VerificationType.PHONE -> "휴대폰 센서"
        VerificationType.HEALTH_CONNECT -> "건강 데이터"
        VerificationType.EXTERNAL -> "외부 서비스"
        VerificationType.MANUAL -> "직접 체크"
    }

private fun SignalSource.label(): String =
    when (this) {
        SignalSource.GPS -> "위치(GPS)"
        SignalSource.GEOFENCE -> "지오펜스"
        SignalSource.PHOTO -> "사진"
        SignalSource.GROUP_CHECK -> "그룹 체크"
        SignalSource.UNKNOWN -> "기타"
    }

private fun WearableRequirement.label(): String =
    when (this) {
        WearableRequirement.NONE -> "불필요"
        WearableRequirement.OPTIONAL -> "선택"
        WearableRequirement.REQUIRED -> "필수"
    }

/** 선택된 인증 옵션의 상세(스냅샷) 안내 카드. */
@Composable
private fun VerificationSnapshotCard(
    option: VerificationOption,
    rationale: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "인증 상세",
                    color = RuleUpTheme.colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (option.recommended) {
                    SmallBadge(text = "추천", background = RuleUpTheme.colors.brand, textColor = Color.White)
                }
            }
            SnapshotRow(label = "인증 수단", value = option.verificationType.label())
            option.signalSource?.let { SnapshotRow(label = "신호원", value = it.label()) }
            option.externalService?.let { SnapshotRow(label = "외부 서비스", value = it) }
            SnapshotRow(label = "웨어러블", value = option.wearableRequirement.label())
            if (option.requiredPermissions.isNotEmpty()) {
                SnapshotRow(label = "필요 권한", value = option.requiredPermissions.joinToString(", "))
            }
        }
        rationale?.takeIf { it.isNotBlank() }?.let {
            InfoNote(
                emoji = "💡",
                text = it,
                background = RuleUpPalette.Violet100,
                textColor = RuleUpTheme.colors.textSlate,
            )
        }
    }
}

@Composable
private fun SnapshotRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = RuleUpTheme.colors.textSecondary, fontSize = 11.sp)
        Text(
            value,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PenaltySection(
    state: CreateChallengeState,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(text = "패널티")
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.large),
        ) {
            PenaltyRow(
                spec =
                    PenaltyRowSpec(
                        emoji = "🌡",
                        tileBackground = Color(0xFFFFF1F2),
                        title = "매너 온도 차감",
                        subtitle = "실패 1회당 −${state.mannerDeduction}℃",
                        required = true,
                    ),
                checked = true,
                // 필수 패널티라 끌 수 없다.
                enabled = false,
            )
            PenaltyDivider()
            PenaltyRow(
                spec =
                    PenaltyRowSpec(
                        emoji = "📣",
                        tileBackground = RuleUpTheme.colors.brandSoft,
                        title = "SNS 공유",
                        subtitle = "실패 시 친구에게 자동 공유",
                        required = false,
                    ),
                checked = state.snsShareEnabled,
                onCheckedChange = { onIntent(CreateChallengeIntent.SetSnsShareEnabled(it)) },
            )
            if (state.snsShareEnabled) {
                SnsPhoneField(phone = state.snsPhone, onIntent = onIntent)
            }
            PenaltyDivider()
            PenaltyRow(
                spec =
                    PenaltyRowSpec(
                        emoji = "👥",
                        tileBackground = RuleUpPalette.Violet100,
                        title = "그룹 내 공유",
                        subtitle = "그룹 멤버에게 결과 알림",
                        required = true,
                    ),
                checked = state.groupShare,
                onCheckedChange = { onIntent(CreateChallengeIntent.SetGroupShare(it)) },
            )
        }
    }
}

@Composable
private fun PenaltyDivider() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(RuleUpTheme.colors.border),
    )
}

/** [PenaltyRow] 의 정적 표시 정보. */
private data class PenaltyRowSpec(
    val emoji: String,
    val tileBackground: Color,
    val title: String,
    val subtitle: String,
    val required: Boolean,
)

@Composable
private fun PenaltyRow(
    spec: PenaltyRowSpec,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(spec.tileBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(spec.emoji, fontSize = 18.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        spec.title,
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (spec.required) {
                        SmallBadge(
                            text = "필수",
                            background = RuleUpTheme.colors.textMuted,
                            textColor = Color.White,
                        )
                    } else {
                        SmallBadge(
                            text = "선택",
                            background = RuleUpTheme.colors.surfaceVariant,
                            textColor = RuleUpTheme.colors.textSecondary,
                        )
                    }
                }
                Text(spec.subtitle, color = RuleUpTheme.colors.textSecondary, fontSize = 11.sp)
            }
        }
        GradientSwitch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SnsPhoneField(
    phone: String,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📱", fontSize = 11.sp)
            Text(
                "공유받을 전화번호",
                color = RuleUpTheme.colors.textSlate,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RuleUpTheme.shapes.small)
                    .background(RuleUpTheme.colors.background)
                    .border(2.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.small)
                    .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = phone,
                onValueChange = { onIntent(CreateChallengeIntent.SetSnsPhone(it)) },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = RuleUpTheme.colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (phone.isEmpty()) {
                        Text("010-0000-0000", color = RuleUpTheme.colors.textMuted, fontSize = 14.sp)
                    }
                    inner()
                },
            )
            // 단순 형식 검사 표시. 실제 번호 인증은 추후 서버 연동.
            if (phone.length >= 12) {
                Row(
                    modifier =
                        Modifier
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RuleUpTheme.colors.successContainer)
                            .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "✓",
                        color = RuleUpTheme.colors.onSuccess,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "인증됨",
                        color = RuleUpTheme.colors.onSuccess,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.36.sp,
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("💡", fontSize = 10.sp)
            Text(
                "실패 시 이 번호로 결과가 카카오톡으로 전송돼요",
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 10.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun ConfirmBottomBar(
    isRecommending: Boolean,
    isCreating: Boolean,
    onIntent: (CreateChallengeIntent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card)
                    .singleClickable(enabled = !isRecommending && !isCreating) {
                        onIntent(CreateChallengeIntent.Recommend)
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (isRecommending) "추천 중..." else "다시 추천",
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier =
                Modifier
                    .weight(2f)
                    .height(56.dp)
                    .clip(RuleUpTheme.shapes.card)
                    .background(RuleUpTheme.colors.brand)
                    .singleClickable(enabled = !isRecommending && !isCreating) {
                        onIntent(CreateChallengeIntent.Create)
                    },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isCreating) "만드는 중..." else "이대로 만들기",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Preview
@Composable
private fun ChallengeConfirmPreview() {
    ChallengeFlowPreview {
        ChallengeConfirmContent(
            onIntent = {},
            state =
                CreateChallengeState.initial.copy(
                    title = "매일 아침 6시 기상",
                    description = "아침형 인간이 되어 하루를 길게 쓰는 습관을 만들어요",
                    hasRecommendation = true,
                    matched = true,
                    participationType = ParticipationType.GROUP,
                    repeatDays = listOf(RepeatDay.MON, RepeatDay.TUE, RepeatDay.WED, RepeatDay.THU, RepeatDay.FRI),
                    startDate = "2026-06-01",
                    selectedMethod = SelectedMethod.AUTO,
                    options =
                        listOf(
                            VerificationOption(
                                method = SelectedMethod.AUTO,
                                recommended = true,
                                verificationType = VerificationType.PHONE,
                                signalSource = SignalSource.GPS,
                                wearableRequirement = WearableRequirement.OPTIONAL,
                                externalService = null,
                                requiredPermissions = listOf("LOCATION"),
                            ),
                            VerificationOption(
                                method = SelectedMethod.MANUAL,
                                recommended = false,
                                verificationType = VerificationType.MANUAL,
                                signalSource = null,
                                wearableRequirement = WearableRequirement.NONE,
                                externalService = null,
                                requiredPermissions = emptyList(),
                            ),
                        ),
                    params =
                        listOf(
                            ParamSpec(
                                key = "distance_km",
                                kind = ParamKind.NUMBER,
                                value = ParamValue.Num(5.0),
                                defaultValue = ParamValue.Num(3.0),
                                unit = "km",
                                min = 1.0,
                                max = 42.0,
                            ),
                        ),
                    rationale = "지정 장소에서 30분 이상 머무르면 자동으로 인증돼요",
                    snsShareEnabled = true,
                    snsPhone = "010-1234-5678",
                ),
        )
    }
}
