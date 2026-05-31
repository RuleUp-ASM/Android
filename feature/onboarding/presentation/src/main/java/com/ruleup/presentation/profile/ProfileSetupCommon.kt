package com.ruleup.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.theme.RuleUpColors

/** 프로필 설정 화면 상단 제목 + 보조 설명. */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleSize: Int = 23,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            color = RuleUpColors.TextPrimary,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            color = RuleUpColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )
    }
}

/** 화면 하단의 안내 박스(아이콘 + 설명). */
@Composable
fun InfoBox(
    background: Color,
    emoji: String,
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, color = textColor, fontSize = 11.sp, lineHeight = 16.sp)
    }
}
