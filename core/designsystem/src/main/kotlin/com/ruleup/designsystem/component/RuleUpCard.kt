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

/** 화면을 구획하는 카드 표면. */
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

/** 카드 표면만 입힌다. */
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
