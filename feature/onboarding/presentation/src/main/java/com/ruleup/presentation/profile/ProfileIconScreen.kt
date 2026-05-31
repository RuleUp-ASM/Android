package com.ruleup.presentation.profile

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.ProfileSetupScaffold
import com.ruleup.core.designsystem.theme.RuleUpColors
import com.ruleup.core.designsystem.theme.RuleUpGradients

private val initialPalette = listOf(
    Brush.linearGradient(listOf(RuleUpColors.Indigo, RuleUpColors.Purple)),
    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFF43F5E))),
    Brush.linearGradient(listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))),
    Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
    Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFBE123C))),
)

/** 01 · 프로필 아이콘 (1/4). */
@Composable
fun ProfileIconScreen(
    modifier: Modifier = Modifier,
    initial: String = "준",
    selectedPalette: Int = 0,
) {
    ProfileSetupScaffold(step = 0, buttonText = "다음", modifier = modifier) {
        SectionHeader(
            title = "프로필 아이콘을 골라주세요",
            subtitle = "사진을 올리거나 기본 아이콘을 사용할 수 있어요",
        )

        // 메인 아바타 + 카메라 뱃지
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(70.dp))
                    .background(RuleUpGradients.Brand)
                    .border(5.dp, Color.White, RoundedCornerShape(70.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(2.dp, RuleUpColors.Indigo, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("📷", fontSize = 18.sp)
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
            )
            SourceCard(
                modifier = Modifier.weight(1f),
                iconBackground = RuleUpGradients.Warm,
                emoji = "🖼️",
                title = "갤러리에서 선택",
                caption = "앨범에서 고르기",
            )
        }

        // 기본 아이콘 라벨
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "기본 아이콘",
                color = RuleUpColors.TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.66.sp,
            )
            Text("이니셜로 만들어드려요", color = RuleUpColors.TextMuted, fontSize = 10.sp)
        }

        // 이니셜 팔레트
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            initialPalette.forEachIndexed { index, brush ->
                val selected = index == selectedPalette
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .background(brush)
                        .then(
                            if (selected) {
                                Modifier.border(3.dp, RuleUpColors.Indigo, RoundedCornerShape(27.dp))
                            } else {
                                Modifier
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(initial, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        InfoBox(
            background = RuleUpColors.IndigoTint,
            emoji = "💡",
            text = "권장 1024×1024 · 최대 10MB · JPG/PNG",
            textColor = RuleUpColors.Indigo700,
        )
    }
}

@Composable
private fun SourceCard(
    iconBackground: Brush,
    emoji: String,
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(RuleUpColors.Surface)
            .border(1.dp, RuleUpColors.Border, RoundedCornerShape(16.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 20.sp)
        }
        Text(
            title,
            modifier = Modifier.padding(top = 8.dp),
            color = RuleUpColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(caption, color = RuleUpColors.TextSecondary, fontSize = 10.sp)
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ProfileIconScreenPreview() {
    ProfileIconScreen()
}
