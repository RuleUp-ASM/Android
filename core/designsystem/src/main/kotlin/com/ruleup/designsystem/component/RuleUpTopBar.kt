package com.ruleup.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.R
import com.ruleup.designsystem.singleClickable
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 뒤로가기 + 제목 상단바.
 *
 * 뒤로가기는 40dp 박스 안에 22dp 아이콘을 넣는다 — 아이콘 크기 그대로 두면 터치 영역이 권장치
 * (48dp)에 한참 못 미친다.
 *
 * @param trailing 오른쪽 끝에 붙일 것(더보기 메뉴 등). 제목과 사이는 이 함수가 벌린다.
 */
@Composable
fun RuleUpTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .singleClickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = "뒤로",
                tint = RuleUpTheme.colors.textPrimary,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            color = RuleUpTheme.colors.textPrimary,
            style = RuleUpTheme.typography.section,
        )
        trailing()
    }
}

@Preview
@Composable
private fun RuleUpTopBarPreview() {
    RuleUpTheme {
        RuleUpTopBar(title = "공지", onBack = {})
    }
}
