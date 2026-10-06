package com.ruleup.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/** 하단 탭 항목. */
enum class RuleUpBottomTab(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    HOME("홈", R.drawable.ic_tab_home),
    EXPLORE("탐색", R.drawable.ic_search),
    CHALLENGE("챌린지", R.drawable.ic_tab_challenge),
    MY("마이", R.drawable.ic_person),
}

/**
 * 화면 하단에 떠 있는 탭 바(Figma 1557:2 시안 ⑤). 둥근 캡슐 4탭 + 오른쪽에 따로 떨어진 만들기 버튼.
 * 바탕을 칠하지 않으니, 내용 위에 겹쳐 그리는 화면은 목록 끝에 바 높이만큼 여백을 둬야 한다.
 */
@Composable
fun RuleUpBottomTabBar(
    selected: RuleUpBottomTab,
    onTabClick: (RuleUpBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = RuleUpTheme.colors.brand,
    onCreateClick: (() -> Unit)? = null,
) {
    val capsule = RoundedCornerShape(BarHeight / 2)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(BarHeight)
                    .shadow(12.dp, capsule, ambientColor = BarShadow, spotColor = BarShadow)
                    .clip(capsule)
                    .background(RuleUpTheme.colors.surface)
                    .border(1.dp, RuleUpTheme.colors.border, capsule)
                    .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            RuleUpBottomTab.entries.forEach { tab ->
                BottomTabItem(tab, selected, selectedColor, onTabClick, Modifier.weight(1f))
            }
        }
        if (onCreateClick != null) CreateButton(onClick = onCreateClick)
    }
}

@Composable
private fun CreateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(BarHeight)
                .shadow(12.dp, CircleShape, ambientColor = BarShadow, spotColor = BarShadow)
                .clip(CircleShape)
                .background(RuleUpTheme.colors.brand)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = "챌린지 만들기",
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun BottomTabItem(
    tab: RuleUpBottomTab,
    selected: RuleUpBottomTab,
    selectedColor: Color,
    onTabClick: (RuleUpBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = tab == selected
    val tint = if (isSelected) selectedColor else RuleUpTheme.colors.textMuted
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .clip(RoundedCornerShape(BarHeight / 2))
                .background(if (isSelected) selectedColor.copy(alpha = 0.12f) else Color.Transparent)
                .singleClickable { if (!isSelected) onTabClick(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = tab.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

private val BarHeight = 64.dp
private val BarShadow = Color(0xFF1E1A4D).copy(alpha = 0.18f)

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun RuleUpBottomTabBarPreview() {
    RuleUpTheme {
        RuleUpBottomTabBar(
            selected =
                com.ruleup.designsystem.component.RuleUpBottomTab.entries
                    .first(),
            onTabClick = { },
        )
    }
}
