package com.ruleup.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.ProfileSetupScaffold
import com.ruleup.core.designsystem.theme.RuleUpColors
import com.ruleup.core.designsystem.theme.RuleUpGradients

private data class Interest(val emoji: String, val label: String)

private val interests = listOf(
    Interest("🏃", "운동"), Interest("📚", "독서"), Interest("🧘", "명상"),
    Interest("💧", "건강"), Interest("🌅", "기상"), Interest("💼", "업무"),
    Interest("📖", "학습"), Interest("🎨", "취미"), Interest("🍳", "요리"),
    Interest("💰", "재테크"), Interest("🌱", "환경"), Interest("🤝", "관계"),
    Interest("🎵", "음악"), Interest("✍️", "글쓰기"), Interest("💻", "코딩"),
)

/** 03 · 관심 분야 (3/4). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestScreen(
    modifier: Modifier = Modifier,
    selected: Set<String> = setOf("운동", "독서", "건강", "기상"),
) {
    ProfileSetupScaffold(step = 2, buttonText = "다음", modifier = modifier) {
        SectionHeader(
            title = "어떤 챌린지에 관심 있나요?",
            subtitle = "선택한 분야 기반으로 챌린지를 추천해드려요",
        )

        // 선택 안내 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(RuleUpColors.IndigoTint)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✨", fontSize = 14.sp)
                Text(
                    "최소 3개 이상 선택해주세요",
                    color = RuleUpColors.Indigo700,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                "${selected.size} / 10",
                color = RuleUpColors.Indigo,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // 관심 분야 칩
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            interests.forEach { interest ->
                InterestChip(interest = interest, selected = interest.label in selected)
            }
        }

        InfoBox(
            background = RuleUpColors.PurpleTint,
            emoji = "🤖",
            text = "선택한 분야는 언제든 마이페이지에서 수정할 수 있어요",
            textColor = RuleUpColors.TextSlate,
        )
    }
}

@Composable
private fun InterestChip(interest: Interest, selected: Boolean) {
    val base = Modifier
        .height(42.dp)
        .clip(RoundedCornerShape(21.dp))
    val styled = if (selected) {
        base.background(RuleUpGradients.Button)
    } else {
        base
            .background(RuleUpColors.Surface)
            .border(1.dp, RuleUpColors.Border, RoundedCornerShape(21.dp))
    }
    Row(
        modifier = styled.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(interest.emoji, fontSize = 16.sp)
        Text(
            interest.label,
            color = if (selected) Color.White else RuleUpColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun InterestScreenPreview() {
    InterestScreen()
}
