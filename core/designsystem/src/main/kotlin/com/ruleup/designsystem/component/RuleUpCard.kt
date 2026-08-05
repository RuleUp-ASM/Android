package com.ruleup.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme

/**
 * 화면을 구획하는 카드 표면.
 *
 * 반경 16 · `bg/surface` 채움 · `border/line` 1dp 테두리 · 안쪽 여백 16. 챌린지 상세·방 스레드·
 * 마이·인증 화면이 같은 조합을 각자 적어 두고 있었다(12곳). 값이 하나 바뀌면 그만큼 찾아다녀야
 * 했다.
 */
@Composable
fun RuleUpCard(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.ruleUpCardSurface(),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * 카드 표면만 입힌다. 세로가 아닌 배치(가로 나열, 눌리는 행)에서 쓴다.
 *
 * 클릭을 붙일 때는 이 함수보다 **먼저** 붙여야 눌림 효과가 여백까지 덮는다 —
 * `Modifier.ruleUpCardSurface()` 가 안쪽 여백을 포함하기 때문이다.
 */
@Composable
fun Modifier.ruleUpCardSurface(): Modifier =
    this
        .fillMaxWidth()
        .clip(RuleUpTheme.shapes.card)
        .background(RuleUpTheme.colors.surface)
        .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card)
        .padding(16.dp)

@Preview
@Composable
private fun RuleUpCardPreview() {
    RuleUpTheme {
        RuleUpCard(modifier = Modifier.padding(12.dp)) {
            androidx.compose.material3.Text(
                text = "카드 표면",
                style = RuleUpTheme.typography.section,
                color = RuleUpTheme.colors.textPrimary,
            )
        }
    }
}
