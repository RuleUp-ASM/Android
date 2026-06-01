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
import com.ruleup.core.designsystem.theme.RuleUpTheme

private data class Permission(
    val emoji: String,
    val iconTint: Color,
    val name: String,
    val required: Boolean,
    val description: String,
    val enabled: Boolean,
)

/** 권한 목록. 아이콘 틴트는 시맨틱 컨테이너 토큰을 써 라이트/다크에 모두 대응한다. */
@Composable
private fun permissionItems(): List<Permission> = listOf(
    Permission("📍", RuleUpTheme.colors.brandSoft, "위치", true, "GPS 인증 챌린지에 사용해요", true),
    Permission("📷", RuleUpTheme.colors.successContainer, "카메라", true, "인증 사진 촬영에 사용해요", true),
    Permission("🔔", RuleUpTheme.colors.warningContainer, "알림", false, "챌린지 시작과 결과를 알려드려요", true),
    Permission("🖼️", RuleUpColors.PurpleTint, "사진", false, "프로필/갤러리 인증에 사용해요", false),
)

/** 04 · 권한 허용 (4/4). */
@Composable
fun PermissionContent(
    modifier: Modifier = Modifier,
    onNext: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    ProfileSetupScaffold(
        step = 3,
        buttonText = "RuleUp 시작하기",
        modifier = modifier,
        onNext = onNext,
        onBack = onBack,
    ) {
        SectionHeader(
            title = "몇 가지 권한이 필요해요",
            subtitle = "RuleUp을 잘 쓰기 위한 권한이에요. 거부해도 일부 기능만 제한돼요",
            titleSize = 22,
        )

        // 권한 카드 묶음
        val permissions = permissionItems()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.card)
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card),
        ) {
            permissions.forEachIndexed { index, permission ->
                PermissionRow(permission)
                if (index != permissions.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RuleUpTheme.colors.border),
                    )
                }
            }
        }

        InfoBox(
            background = RuleUpTheme.colors.successContainer,
            emoji = "🛡",
            text = "수집된 데이터는 인증/추천에만 쓰이고 7일 후 자동 삭제돼요",
            textColor = RuleUpTheme.colors.onSuccess,
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
            horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.md),
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
                    horizontalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(permission.name, color = RuleUpTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    RequirementBadge(required = permission.required)
                }
                Text(
                    permission.description,
                    color = RuleUpTheme.colors.textSecondary,
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
    val background = if (required) RuleUpTheme.colors.danger else RuleUpTheme.colors.surfaceVariant
    val textColor = if (required) Color.White else RuleUpTheme.colors.textSecondary
    Box(
        modifier = Modifier
            .height(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .padding(horizontal = RuleUpTheme.spacing.xs),
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
        modifier = if (on) track.background(RuleUpGradients.Button) else track.background(RuleUpTheme.colors.borderStrong),
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
    RuleUpTheme { PermissionContent() }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun PermissionScreenDarkPreview() {
    RuleUpTheme(darkTheme = true) { PermissionContent() }
}
