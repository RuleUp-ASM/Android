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
import com.ruleup.core.designsystem.theme.RuleUpColors
import com.ruleup.core.designsystem.theme.RuleUpGradients

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
                .clip(RoundedCornerShape(20.dp))
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
                color = RuleUpColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // 닉네임 입력
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("닉네임", color = RuleUpColors.TextSlate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${nickname.length} / $maxLength", color = RuleUpColors.TextMuted, fontSize = 11.sp)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RuleUpColors.Surface)
                    .border(2.dp, RuleUpColors.Indigo, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nickname, color = RuleUpColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Box(
                        Modifier
                            .padding(start = 8.dp)
                            .width(2.dp)
                            .height(20.dp)
                            .background(RuleUpColors.Indigo),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RuleUpColors.Success),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✓", color = RuleUpColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("사용 가능한 닉네임이에요", color = RuleUpColors.SuccessText, fontSize = 11.sp)
            }
        }

        // 닉네임 규칙
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpColors.IndigoTint)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "닉네임 규칙",
                color = RuleUpColors.Indigo700,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.66.sp,
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (ok) "✓" else "✗",
            color = if (ok) RuleUpColors.Success else RuleUpColors.Danger,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text, color = RuleUpColors.TextSlate, fontSize = 11.sp)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun NicknameScreenPreview() {
    NicknameScreen()
}
