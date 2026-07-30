package com.ruleup.onboarding.presentation.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpGradients
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.entity.category.Category
import com.ruleup.onboarding.domain.navigation.ProfilePermissionPage
import com.ruleup.onboarding.presentation.component.ProfileSetupScaffold
import com.ruleup.onboarding.presentation.profile.component.ProfileFlowPreview
import com.ruleup.onboarding.presentation.profile.component.SectionHeader
import com.ruleup.onboarding.presentation.profile.viewmodel.ProfileIntent
import com.ruleup.ui.helper.LocalNavigationHelper

/** 카테고리별 Figma 라인 아이콘. 학습(STUDY)은 Figma 에서 독서와 동일 아이콘을 공유한다. */
private fun Category.icon(): Int =
    when (this) {
        Category.EXERCISE -> com.ruleup.designsystem.R.drawable.ic_cat_exercise
        Category.READING -> com.ruleup.designsystem.R.drawable.ic_cat_reading
        Category.MEDITATION -> com.ruleup.designsystem.R.drawable.ic_cat_meditation
        Category.HEALTH -> com.ruleup.designsystem.R.drawable.ic_cat_health
        Category.WAKE_UP -> com.ruleup.designsystem.R.drawable.ic_cat_wakeup
        Category.WORK -> com.ruleup.designsystem.R.drawable.ic_cat_work
        Category.STUDY -> com.ruleup.designsystem.R.drawable.ic_cat_reading
        Category.HOBBY -> com.ruleup.designsystem.R.drawable.ic_cat_hobby
        Category.COOKING -> com.ruleup.designsystem.R.drawable.ic_cat_cooking
        Category.FINANCE -> com.ruleup.designsystem.R.drawable.ic_cat_finance
        Category.ENVIRONMENT -> com.ruleup.designsystem.R.drawable.ic_cat_environment
        Category.RELATIONSHIP -> com.ruleup.designsystem.R.drawable.ic_cat_relationship
        Category.MUSIC -> com.ruleup.designsystem.R.drawable.ic_cat_music
        Category.WRITING -> com.ruleup.designsystem.R.drawable.ic_cat_writing
        Category.CODING -> com.ruleup.designsystem.R.drawable.ic_cat_coding
    }

/** 03 · 관심 분야 (3/5). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestContent(
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
    selected: List<Category> = listOf(Category.EXERCISE),
) {
    val nav = LocalNavigationHelper.current
    ProfileSetupScaffold(
        step = 2,
        buttonText = "다음",
        modifier = modifier,
        onNext = { nav.navigateTo(ProfilePermissionPage) },
        onBack = { nav.navigateToBack() },
    ) {
        SectionHeader(
            title = "어떤 챌린지에 관심 있나요?",
            subtitle = "선택한 분야 기반으로 챌린지를 추천해드려요",
        )

        SelectionCounter(count = selected.size)

        // 관심 분야 칩 (3열 그리드)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Category.entries.forEach { interest ->
                InterestChip(
                    interest = interest,
                    selected = interest in selected,
                    onClick = { onIntent(ProfileIntent.SetProfileInterest(it)) },
                )
            }
        }

        InfoRow(text = "선택한 분야는 언제든 마이페이지에서 수정할 수 있어요")
    }
}

/** "최소 3개 이상" 안내 + 선택 카운트 바. */
@Composable
private fun SelectionCounter(count: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF2F0FE))
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(com.ruleup.designsystem.R.drawable.ic_info),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color(0xFF5A47D6)),
                modifier = Modifier.size(16.dp),
            )
            Text(
                "최소 3개 이상 선택해주세요",
                color = Color(0xFF5A47D6),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text("$count / 10", color = RuleUpTheme.colors.brand, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InterestChip(
    interest: Category,
    selected: Boolean,
    onClick: (Category) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .width(101.dp)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .then(
                    if (selected) {
                        Modifier.background(RuleUpGradients.Button)
                    } else {
                        Modifier
                            .background(RuleUpTheme.colors.surface)
                            .border(1.dp, RuleUpTheme.colors.border, RoundedCornerShape(21.dp))
                    },
                ).singleClickable(globalGuard = false) { onClick(interest) },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(interest.icon()),
            contentDescription = null,
            colorFilter = if (selected) ColorFilter.tint(Color.White) else null,
            modifier = Modifier.size(16.dp),
        )
        Text(
            interest.label,
            color = if (selected) Color.White else RuleUpTheme.colors.textPrimary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

/** SVG 정보 아이콘이 달린 안내 박스. */
@Composable
private fun InfoRow(text: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF5F3FF))
                .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(com.ruleup.designsystem.R.drawable.ic_info),
            contentDescription = null,
            colorFilter = ColorFilter.tint(RuleUpTheme.colors.textSlate),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text,
            color = RuleUpTheme.colors.textSlate,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Preview
@Composable
private fun InterestScreenPreview() {
    ProfileFlowPreview { InterestContent(onIntent = {}) }
}
