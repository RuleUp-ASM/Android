package com.ruleup.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 하단 탭 항목. 루트 화면(홈·탐색·챌린지·마이)이 공유한다.
 *
 * 순서가 곧 화면 배치다 — 가운데 생성 버튼을 기준으로 앞 둘([HOME]·[EXPLORE])과 뒤 둘
 * ([CHALLENGE]·[MY])로 갈린다.
 */
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
 * 화면 하단 고정 탭 바 (Figma 1134:2062).
 *
 * **생성 버튼이 바 안에 들어있다.** 예전처럼 우측 하단에 따로 띄우면 목록 마지막 카드를 가리고,
 * 화면마다 위치가 조금씩 달라진다. 가운데 고정이면 어느 탭에서든 같은 자리다.
 *
 * [onTabClick] 은 현재 선택된 탭이 아닌 탭을 눌렀을 때만 호출된다.
 */
@Composable
fun RuleUpBottomTabBar(
    selected: RuleUpBottomTab,
    onTabClick: (RuleUpBottomTab) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = RuleUpTheme.colors.brand,
    onCreateClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .navigationBarsPadding(),
    ) {
        HorizontalDivider(thickness = 1.dp, color = RuleUpTheme.colors.border)
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomTabItem(RuleUpBottomTab.HOME, selected, selectedColor, onTabClick)
            BottomTabItem(RuleUpBottomTab.EXPLORE, selected, selectedColor, onTabClick)
            if (onCreateClick != null) {
                CreateButton(onClick = onCreateClick)
            } else {
                // 생성 진입이 없는 화면에서도 탭 좌우 배치가 흔들리지 않게 자리만 지킨다.
                Spacer(Modifier.width(44.dp))
            }
            BottomTabItem(RuleUpBottomTab.CHALLENGE, selected, selectedColor, onTabClick)
            BottomTabItem(RuleUpBottomTab.MY, selected, selectedColor, onTabClick)
        }
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
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(RuleUpTheme.colors.brand)
                .singleClickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_plus),
            contentDescription = "챌린지 만들기",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
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
                .width(56.dp)
                .fillMaxHeight()
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
