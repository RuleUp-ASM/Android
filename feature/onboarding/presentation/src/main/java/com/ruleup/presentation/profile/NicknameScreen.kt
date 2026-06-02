package com.ruleup.presentation.profile

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.ProfileSetupScaffold
import com.ruleup.core.designsystem.theme.RuleUpGradients
import com.ruleup.core.designsystem.theme.RuleUpTheme
import com.ruleup.presentation.profile.util.NickNameUtil

/** 02 · 닉네임 (2/4). */
@Composable
fun NicknameContent(
    modifier: Modifier = Modifier,
    nickname: String = "준혁이의 도전",
    maxLength: Int = 12,
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
    onNickNameChange: (String) -> Unit = {},
) {
    ProfileSetupScaffold(
        step = 1,
        buttonText = "다음",
        modifier = modifier,
        onNext = onNext,
        onBack = onBack,
    ) {
        SectionHeader(
            title = "어떻게 불러드릴까요?",
            subtitle = "친구들에게 보여질 이름이에요",
            titleSize = 24,
        )
        NicknamePreviewCard(nickname = nickname)
        NicknameField(nickname = nickname, maxLength = maxLength, onNickNameChange = onNickNameChange)
        NicknameRules(nickname = nickname)
    }
}

/** 그라데이션 카드 위에 현재 닉네임을 미리 보여준다. */
@Composable
private fun NicknamePreviewCard(nickname: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RuleUpTheme.shapes.cardLarge)
                .background(
                    Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF5F3FF))),
                ),
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
        ) {}
        Text(
            nickname,
            modifier = Modifier.padding(top = 10.dp),
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.title,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 닉네임 입력 필드 + 글자 수 + 유효성 안내. */
@Composable
private fun NicknameField(
    nickname: String,
    maxLength: Int,
    onNickNameChange: (String) -> Unit,
) {
    val valid = NickNameUtil.isValid(nickname)
    Column(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("닉네임", color = RuleUpTheme.colors.textSlate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("${nickname.length} / $maxLength", color = RuleUpTheme.colors.textMuted, fontSize = 11.sp)
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
                textStyle = RuleUpTheme.typography.bodyLarge.copy(color = RuleUpTheme.colors.textPrimary),
                cursorBrush = SolidColor(RuleUpTheme.colors.brand),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    if (nickname.isEmpty()) {
                        Text(
                            "닉네임을 입력하세요",
                            color = RuleUpTheme.colors.textMuted,
                            style = RuleUpTheme.typography.bodyLarge,
                        )
                    }
                    inner()
                },
            )
            if (nickname.isNotEmpty()) {
                StatusBadge(valid = valid)
            }
        }
        if (nickname.isNotEmpty()) {
            NicknameStatusMessage(valid = valid)
        }
    }
}

/** 입력 필드 우측의 ✓/✕ 뱃지. */
@Composable
private fun StatusBadge(valid: Boolean) {
    Box(
        modifier =
            Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (valid) RuleUpTheme.colors.success else RuleUpTheme.colors.danger),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (valid) "✓" else "✕",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** 입력 필드 아래의 유효성 메시지. */
@Composable
private fun NicknameStatusMessage(valid: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (valid) "✓" else "✕",
            color = if (valid) RuleUpTheme.colors.success else RuleUpTheme.colors.danger,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (valid) "사용 가능한 닉네임이에요" else "한글·영문·숫자 2~12자만 가능해요",
            color = if (valid) RuleUpTheme.colors.onSuccess else RuleUpTheme.colors.danger,
            fontSize = 11.sp,
        )
    }
}

/** 닉네임 규칙 안내 박스. */
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
            style = RuleUpTheme.typography.overline,
        )
        RuleRow(ok = inRange, "2 ~ 12자 사이")
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
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text, color = RuleUpTheme.colors.textSlate, fontSize = 11.sp)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun NicknameScreenPreview() {
    RuleUpTheme { NicknameContent() }
}
