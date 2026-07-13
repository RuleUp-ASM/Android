package com.ruleup.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.ui.R
import com.ruleup.ui.helper.singleClickable
import com.ruleup.ui.theme.RuleUpTheme

/** 하단 탭 항목. 홈·탐색 등 루트 화면이 공유한다(미구현 탭은 자리만 차지). */
enum class RuleUpBottomTab(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    HOME("홈", R.drawable.ic_tab_home),
    CHALLENGE("챌린지", R.drawable.ic_tab_challenge),
    GROUP("그룹", R.drawable.ic_tab_group),
    MY("마이", R.drawable.ic_person),
}

/**
 * 화면 하단 고정 탭 바. 홈/탐색이 같은 규칙을 쓰므로 core:ui 로 모은다.
 * [onTabClick] 은 현재 선택된 탭이 아닌 탭을 눌렀을 때만 호출된다.
 */
@Composable
fun RuleUpBottomTabBar(
    selected: RuleUpBottomTab,
    onTabClick: (RuleUpBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = RuleUpTheme.colors.brand,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .border(1.dp, RuleUpTheme.colors.border)
                .navigationBarsPadding()
                .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RuleUpBottomTab.entries.forEach { tab ->
            BottomTabItem(
                tab = tab,
                selected = tab == selected,
                selectedColor = selectedColor,
                modifier = Modifier.weight(1f),
                onClick = { if (tab != selected) onTabClick(tab) },
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: RuleUpBottomTab,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) selectedColor else RuleUpTheme.colors.textMuted
    Column(
        modifier =
            modifier
                .fillMaxHeight()
                .singleClickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = tab.label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
