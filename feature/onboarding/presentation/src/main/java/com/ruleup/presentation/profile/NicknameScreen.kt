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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.ProfileSetupScaffold
import com.ruleup.core.designsystem.theme.RuleUpGradients
import com.ruleup.core.designsystem.theme.RuleUpTheme

/** 02 · 닉네임 (2/4). */
@Composable
fun NicknameScreen(
    modifier: Modifier = Modifier,
    nickname: String = "준혁이의 도전",
    initial: String = "준",
    maxLength: Int = 12,
) {
    ProfileSetupScaffold(step = 1, buttonText = "다음", modifier = modifier) {
        SectionHeader(
            title = "어떻게 불러드릴까요?",
            subtitle = "친구들에게 보여질 이름이에요",
            titleSize = 24,
        )

        // 미리보기 카드
        Column(
            modifier = Modifier
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
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(RuleUpGradients.Brand)
                    .border(3.dp, Color.White, RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                nickname,
                modifier = Modifier.padding(top = 10.dp),
                color = RuleUpTheme.colors.textPrimary,
                style = RuleUpTheme.typography.title,
                fontWeight = FontWeight.Bold,
            )
        }

        // 닉네임 입력
        Column(verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("닉네임", color = RuleUpTheme.colors.textSlate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${nickname.length} / $maxLength", color = RuleUpTheme.colors.textMuted, fontSize = 11.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RuleUpTheme.shapes.large)
                    .background(RuleUpTheme.colors.surface)
                    .border(2.dp, RuleUpTheme.colors.brand, RuleUpTheme.shapes.large)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nickname, color = RuleUpTheme.colors.textPrimary, style = RuleUpTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Box(
                        Modifier
                            .padding(start = RuleUpTheme.spacing.sm)
                            .width(2.dp)
                            .height(20.dp)
                            .background(RuleUpTheme.colors.brand),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RuleUpTheme.colors.success),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✓", color = RuleUpTheme.colors.success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("사용 가능한 닉네임이에요", color = RuleUpTheme.colors.onSuccess, fontSize = 11.sp)
            }
        }

        // 닉네임 규칙
        Column(
            modifier = Modifier
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
            RuleRow(ok = true, "2 ~ 12자 사이")
            RuleRow(ok = true, "한글, 영문, 숫자 사용 가능")
            RuleRow(ok = false, "특수문자 및 공백 불가")
        }
    }
}

@Composable
private fun RuleRow(ok: Boolean, text: String) {
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
    RuleUpTheme { NicknameScreen() }
}
