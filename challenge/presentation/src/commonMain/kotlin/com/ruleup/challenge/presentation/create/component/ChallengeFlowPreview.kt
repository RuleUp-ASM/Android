package com.ruleup.challenge.presentation.create.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.NavigationHelperImpl
import com.ruleup.ui.theme.RuleUpTheme

/**
 * 챌린지 생성 Content 프리뷰용 래퍼. Content 가 직접 읽는 [LocalNavigationHelper] 를 더미로 제공한다.
 * (이미지 피커는 actual 의 [androidx.compose.ui.platform.LocalInspectionMode] 가드로 프리뷰에서 no-op 이므로
 * 별도의 ActivityResultRegistry 더미가 필요 없다.)
 */
@Composable
internal fun ChallengeFlowPreview(content: @Composable () -> Unit) {
    RuleUpTheme {
        CompositionLocalProvider(
            LocalNavigationHelper provides NavigationHelperImpl(),
        ) {
            content()
        }
    }
}
