package com.ruleup.onboarding.presentation.profile.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.NavigationHelperImpl
import com.ruleup.ui.theme.RuleUpTheme

/**
 * 프로필 설정 Content 프리뷰용 래퍼. Content 가 직접 읽는 [LocalNavigationHelper] 를 더미로 제공한다.
 * (예전 Android 전용 ActivityResultRegistry 더미는 더 이상 읽는 곳이 없어 제거됨.)
 */
@Composable
internal fun ProfileFlowPreview(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    RuleUpTheme(darkTheme = darkTheme) {
        CompositionLocalProvider(
            LocalNavigationHelper provides NavigationHelperImpl(),
        ) {
            content()
        }
    }
}

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
        verticalArrangement = Arrangement.spacedBy(RuleUpTheme.spacing.sm),
    ) {
        Text(
            title,
            color = RuleUpTheme.colors.textPrimary,
            fontSize = titleSize.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            color = RuleUpTheme.colors.textSecondary,
            style = RuleUpTheme.typography.label,
            fontWeight = FontWeight.Normal,
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
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.medium)
                .background(background)
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 16.sp)
        Text(text, color = textColor, style = RuleUpTheme.typography.caption)
    }
}

/** 필수/선택 배지. 권한·약관 등 프로필 설정 단계들이 공유한다. */
@Composable
fun RequirementBadge(
    required: Boolean,
    modifier: Modifier = Modifier,
) {
    val background = if (required) RuleUpTheme.colors.danger else RuleUpTheme.colors.surfaceVariant
    val textColor = if (required) Color.White else RuleUpTheme.colors.textSecondary
    Box(
        modifier =
            modifier
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

/** 카드 내부 항목 사이의 가로 구분선. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(RuleUpTheme.colors.border),
    )
}
