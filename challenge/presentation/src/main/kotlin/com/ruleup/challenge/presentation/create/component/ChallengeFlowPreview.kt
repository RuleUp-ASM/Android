package com.ruleup.challenge.presentation.create.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.NoOpNavigationHelper

/** 프리뷰 래퍼 — Content 가 직접 읽는 [LocalNavigationHelper] 를 더미로 채운다. */
@Composable
internal fun ChallengeFlowPreview(content: @Composable () -> Unit) {
    RuleUpTheme {
        CompositionLocalProvider(
            LocalNavigationHelper provides NoOpNavigationHelper,
        ) {
            content()
        }
    }
}
