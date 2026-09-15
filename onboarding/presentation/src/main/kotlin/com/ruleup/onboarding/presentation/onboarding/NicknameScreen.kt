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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.user.NickNameUtil
import com.ruleup.onboarding.domain.navigation.OnboardingInterestPage
import com.ruleup.onboarding.domain.observability.OnboardingStep
import com.ruleup.onboarding.presentation.component.OnboardingScaffold
import com.ruleup.onboarding.presentation.onboarding.component.OnboardingFlowPreview
import com.ruleup.onboarding.presentation.onboarding.component.SectionHeader
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/** 01 · 닉네임. "다음" 은 ViewModel 의 닉네임 검사를 거쳐 통과 시 ViewModel 이 이동시킨다. */
@Composable
fun NicknameContent(
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier,
    nickname: String = "",
    nicknameMessage: String? = null,
    nicknameAvailable: Boolean? = null,
    maxLength: Int = NickNameUtil.MAX_LENGTH,
    imageUri: String? = null,
) {
    val nav = LocalNavigationHelper.current
    OnboardingScaffold(
        step = OnboardingStep.NICKNAME,
        buttonText = "다음",
        modifier = modifier,
        // 서버 확인까지 통과해야 넘어간다. 통과 전에 전진시키면 마지막 제출에서 1단계로 되돌아온다.
        nextEnabled = nicknameAvailable == true,
        onNext = { nav.navigateTo(OnboardingInterestPage) },
        // 1단계 뒤로가기는 곧 이탈이다. signupToken 은 5분이라 되돌아올 수 없다.
        onBack = { onIntent(OnboardingIntent.BackFromFirstStep) },
    ) {
        SectionHeader(
            title = "어떻게 불러드릴까요?",
            subtitle = "친구들에게 보여질 이름이에요",
        )
        NicknamePreviewCard(nickname = nickname, imageUri = imageUri)
        NicknameField(
            nickname = nickname,
            maxLength = maxLength,
            feedback = nicknameFeedback(nickname, nicknameMessage, nicknameAvailable),
            onNickNameChange = { onIntent(OnboardingIntent.SetNickName(it)) },
        )
        NicknameRules(nickname = nickname)
    }
}

/** 그라데이션 카드 위에 현재 닉네임을 미리 보여준다. 앞 단계에서 사진을 골랐다면 그 사진을 아바타로 쓴다. */
@Composable
private fun NicknamePreviewCard(
    nickname: String,
    imageUri: String?,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RuleUpTheme.shapes.cardLarge)
                .background(RuleUpTheme.colors.brandSoft),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(RuleUpGradients.Brand)
                    .border(3.dp, Color.White, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUri.isNullOrEmpty()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(32.dp)),
                )
            } else {
                Image(
                    painter = painterResource(com.ruleup.designsystem.R.drawable.ic_person),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(29.dp),
                )
            }
        }
        Text(
            nickname,
            modifier = Modifier.padding(top = 10.dp),
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
        )
    }
}

/** 입력칸 아래 한 줄 안내. [positive] 면 통과 색, 아니면 경고 색으로 그린다. */
internal data class NicknameFeedback(
    val message: String,
    val positive: Boolean,
)

/**
 * 형식 검사와 서버 확인 결과를 **한 줄로** 합친다. 둘을 따로 그리면 "사용 가능한 닉네임이에요"가 두 번 뜬다.
 * 형식이 틀리면 서버에 묻기 전이라 형식 안내가 우선이고, 형식이 맞으면 서버 확인 결과를 보여 준다.
 */
internal fun nicknameFeedback(
    nickname: String,
    serverMessage: String?,
    serverAvailable: Boolean?,
): NicknameFeedback? {
    if (nickname.isEmpty()) return null
    val validation = NickNameUtil.validate(nickname)
    if (!validation.isValid) return NicknameFeedback(NickNameUtil.message(validation), positive = false)
    return serverMessage?.let { NicknameFeedback(it, positive = serverAvailable == true) }
}

@Composable
private fun NicknameField(
    nickname: String,
    maxLength: Int,
    feedback: NicknameFeedback?,
    onNickNameChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("닉네임", color = RuleUpTheme.colors.textSlate, style = RuleUpTheme.typography.smallBold)
            Text("${nickname.length} / $maxLength", color = RuleUpTheme.colors.textMuted, style = RuleUpTheme.typography.caption)
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(2.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.large)
                    .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = nickname,
                onValueChange = onNickNameChange,
                singleLine = true,
                textStyle = RuleUpTheme.typography.labelMedium.copy(color = RuleUpTheme.colors.textPrimary),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (nickname.isEmpty()) {
                        Text(
                            "닉네임을 입력하세요",
                            color = RuleUpTheme.colors.textMuted,
                            style = RuleUpTheme.typography.labelMedium,
                        )
                    }
                    inner()
                },
            )
            if (nickname.isNotEmpty()) {
                ClearButton(onClick = { onNickNameChange("") })
            }
        }
        if (feedback != null) {
            Text(
                feedback.message,
                color = if (feedback.positive) RuleUpTheme.colors.onSuccess else RuleUpTheme.colors.danger,
                style = RuleUpTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun ClearButton(onClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpTheme.colors.textMuted)
                .semantics { contentDescription = "닉네임 지우기" }
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("✕", color = Color.White, style = RuleUpTheme.typography.captionBold)
    }
}

@Composable
private fun NicknameRules(nickname: String) {
    val inRange = NickNameUtil.inRange(nickname)
    val validType = NickNameUtil.isValidName(nickname)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(RuleUpTheme.colors.brandSoft)
                .padding(RuleUpTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        Text(
            "닉네임 규칙",
            color = RuleUpTheme.colors.brandStrong,
            style = RuleUpTheme.typography.captionBold,
        )
        RuleRow(ok = inRange, "${NickNameUtil.MIN_LENGTH} ~ ${NickNameUtil.MAX_LENGTH}자 사이")
        RuleRow(ok = validType, "한글, 영문, 숫자 사용 가능")
        RuleRow(ok = validType, "특수문자 및 공백 불가")
    }
}

@Composable
private fun RuleRow(
    ok: Boolean,
    text: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (ok) "✓" else "✗",
            color = if (ok) RuleUpTheme.colors.success else RuleUpTheme.colors.danger,
            style = RuleUpTheme.typography.captionBold,
        )
        Text(text, color = RuleUpTheme.colors.textSlate, style = RuleUpTheme.typography.caption)
    }
}

@Preview
@Composable
private fun NicknameScreenPreview() {
    OnboardingFlowPreview { NicknameContent(onIntent = {}) }
}
