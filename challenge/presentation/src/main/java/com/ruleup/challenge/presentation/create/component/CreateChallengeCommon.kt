package com.ruleup.challenge.presentation.create.component

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.core.app.ActivityOptionsCompat
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.NavigationHelperImpl
import com.ruleup.ui.theme.RuleUpGradients
import com.ruleup.ui.theme.RuleUpTheme

/** 챌린지 생성 화면 상단 바: 뒤로(‹) + 제목 (+ 우측 보조 액션). */
@Composable
fun CreateChallengeTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    trailingText: String? = null,
    onTrailingClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(RuleUpTheme.colors.surface)
                .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹",
                modifier = Modifier.clickable(onClick = onBack),
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                title,
                color = RuleUpTheme.colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        if (trailingText != null) {
            Text(
                trailingText,
                modifier = Modifier.clickable(onClick = onTrailingClick),
                color = RuleUpTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** 섹션 라벨(11sp 트래킹) + 우측 보조 슬롯. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            color = RuleUpTheme.colors.textMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.66.sp,
        )
        trailing()
    }
}

/** 작은 캡슐 배지 (선택/필수/추천/AI 선택 등). */
@Composable
fun SmallBadge(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background)
                .padding(horizontal = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = textColor,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.32.sp,
        )
    }
}

/** 브랜드 그라데이션 스위치 (44×26). */
@Composable
fun GradientSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .width(44.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .let { base ->
                    if (checked) {
                        base.background(RuleUpGradients.Button)
                    } else {
                        base.background(RuleUpTheme.colors.borderStrong)
                    }
                }.clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(horizontal = 3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White),
        )
    }
}

/** 안내 한 줄(이모지 + 문구) 박스. */
@Composable
fun InfoNote(
    emoji: String,
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RuleUpTheme.shapes.small)
                .background(background)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(text, color = textColor, fontSize = 11.sp, lineHeight = 15.sp)
    }
}

/** 프리뷰에는 Activity 가 없으므로 런처 등록만 받아 주는 빈 레지스트리를 제공한다. */
private val previewActivityResultRegistryOwner =
    object : ActivityResultRegistryOwner {
        override val activityResultRegistry =
            object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) = Unit
            }
    }

/** 챌린지 생성 Content 프리뷰용 래퍼. [LocalNavigationHelper] 와 런처 레지스트리를 더미로 제공한다. */
@Composable
internal fun ChallengeFlowPreview(content: @Composable () -> Unit) {
    RuleUpTheme {
        CompositionLocalProvider(
            LocalNavigationHelper provides NavigationHelperImpl(),
            LocalActivityResultRegistryOwner provides previewActivityResultRegistryOwner,
        ) {
            content()
        }
    }
}
