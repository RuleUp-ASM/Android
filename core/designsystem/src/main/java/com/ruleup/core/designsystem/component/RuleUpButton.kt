package com.ruleup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.ruleup.core.designsystem.theme.RuleUpGradients

/** Design System v2.0 기본 CTA 버튼(그라데이션 채움). */
@Composable
fun PrimaryGradientButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Int = 52,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RuleUpGradients.Button)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
