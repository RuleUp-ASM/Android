package com.ruleup.onboarding.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ruleup.designsystem.theme.RuleUpTheme

/** 화면 하단 고정 CTA 영역. */
@Composable
fun BottomBar(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(20.dp),
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(RuleUpTheme.colors.surface)
                .padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
