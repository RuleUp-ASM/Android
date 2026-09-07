package com.ruleup.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme

/** 화면을 구획하는 카드 표면. 반경·테두리·여백 조합의 단일 소스 — 화면에서 따로 조립하지 않는다. */
@Composable
fun RuleUpCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = CardPadding,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.ruleUpCardSurface(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content,
    )
}

/**
 * 카드 표면만 입힌다. 세로가 아닌 배치(가로 나열, 눌리는 행)에서 쓴다.
 * 안쪽 여백까지 포함하므로 클릭은 이 함수보다 **먼저** 붙여야 눌림 효과가 여백을 덮는다.
 */
@Composable
fun Modifier.ruleUpCardSurface(contentPadding: PaddingValues = CardPadding): Modifier =
    this
        .fillMaxWidth()
        .clip(RuleUpTheme.shapes.card)
        .background(RuleUpTheme.colors.surface)
        .border(1.dp, RuleUpTheme.colors.border, RuleUpTheme.shapes.card)
        .padding(contentPadding)

private val CardPadding = PaddingValues(16.dp)

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
