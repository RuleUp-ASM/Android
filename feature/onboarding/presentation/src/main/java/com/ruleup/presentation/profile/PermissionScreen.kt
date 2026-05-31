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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.core.designsystem.component.ProfileSetupScaffold
import com.ruleup.core.designsystem.theme.RuleUpColors
import com.ruleup.core.designsystem.theme.RuleUpGradients

private data class Permission(
    val emoji: String,
    val iconTint: Color,
    val name: String,
    val required: Boolean,
    val description: String,
    val enabled: Boolean,
)

private val permissions = listOf(
    Permission("📍", RuleUpColors.IndigoTint, "위치", true, "GPS 인증 챌린지에 사용해요", true),
    Permission("📷", RuleUpColors.GreenTint, "카메라", true, "인증 사진 촬영에 사용해요", true),
    Permission("🔔", RuleUpColors.AmberTint, "알림", false, "챌린지 시작과 결과를 알려드려요", true),
    Permission("🖼️", RuleUpColors.PurpleTint, "사진", false, "프로필/갤러리 인증에 사용해요", false),
)

/** 04 · 권한 허용 (4/4). */
@Composable
fun PermissionScreen(modifier: Modifier = Modifier) {
    ProfileSetupScaffold(step = 3, buttonText = "RuleUp 시작하기", modifier = modifier) {
        SectionHeader(
            title = "몇 가지 권한이 필요해요",
            subtitle = "RuleUp을 잘 쓰기 위한 권한이에요. 거부해도 일부 기능만 제한돼요",
            titleSize = 22,
        )

        // 권한 카드 묶음
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(RuleUpColors.Surface)
                .border(1.dp, RuleUpColors.Border, RoundedCornerShape(16.dp)),
        ) {
            permissions.forEachIndexed { index, permission ->
                PermissionRow(permission)
                if (index != permissions.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RuleUpColors.Border),
                    )
                }
            }
        }

        InfoBox(
            background = RuleUpColors.GreenTint,
            emoji = "🛡",
            text = "수집된 데이터는 인증/추천에만 쓰이고 7일 후 자동 삭제돼요",
            textColor = RuleUpColors.SuccessText,
        )
    }
}

@Composable
private fun PermissionRow(permission: Permission) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(permission.iconTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(permission.emoji, fontSize = 20.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(permission.name, color = RuleUpColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    RequirementBadge(required = permission.required)
                }
                Text(
                    permission.description,
                    color = RuleUpColors.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
        }
        Toggle(on = permission.enabled)
    }
}

@Composable
private fun RequirementBadge(required: Boolean) {
    val background = if (required) RuleUpColors.Danger else Color(0xFFF1F5F9)
    val textColor = if (required) Color.White else RuleUpColors.TextSecondary
    Box(
        modifier = Modifier
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (required) "필수" else "선택",
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.36.sp,
        )
    }
}

@Composable
private fun Toggle(on: Boolean) {
    val track = Modifier
        .width(44.dp)
        .height(26.dp)
        .clip(RoundedCornerShape(13.dp))
    Box(
        modifier = if (on) track.background(RuleUpGradients.Button) else track.background(RuleUpColors.BorderLight),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = 3.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White),
        )
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun PermissionScreenPreview() {
    PermissionScreen()
}
