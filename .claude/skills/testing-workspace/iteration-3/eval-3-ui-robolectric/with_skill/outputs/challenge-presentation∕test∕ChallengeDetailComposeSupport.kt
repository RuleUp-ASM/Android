package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.designsystem.theme.RuleUpTheme
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * 상세 화면 Robolectric 테스트의 공통 배선.
 *
 * 여기 있는 건 전부 **환경을 프로덕션과 같게 맞추는 일**이지 검증이 아니다. 특히 시계 전진은
 * 없으면 클릭이 통째로 삼켜져 "화면이 의도를 안 올린다"는 잘못된 결론으로 이어진다.
 */

/**
 * 전역 클릭 가드([com.ruleup.designsystem.SingleClickGuard])를 넘긴다.
 *
 * 가드는 `object` 라 JVM 이 사는 동안 테스트를 건너 살아남는데, Robolectric 은
 * `SystemClock.elapsedRealtime()` 을 테스트마다 작은 값으로 되감는다. 그래서 앞 테스트가 밀어 둔
 * 시각보다 이른 시각이 들어와 첫 클릭부터 막히고, 하나만 돌리면 통과하는데 클래스 전체를 돌리면
 * 깨진다. **시계를 단조 증가**시키는 것이 유일한 해법이다 — 프로덕션에 리셋 훅을 뚫으면
 * 릴리스 빌드에도 남는다.
 */
internal fun advanceClockPastGuard() {
    ShadowSystemClock.advanceBy(Duration.ofSeconds(60))
}

/** 한 테스트에서 두 번 이상 누를 때. 누를 때마다 가드(300ms) 너머로 민다. */
internal fun SemanticsNodeInteraction.clickPastGuard(): SemanticsNodeInteraction {
    ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
    return performClick()
}

/**
 * 화면을 띄운다. `RuleUpTheme` 없이 렌더하면 색 토큰이 기본값 없는 `staticCompositionLocalOf` 라
 * 곧바로 터진다.
 */
internal fun ComposeContentTestRule.showDetail(
    state: ChallengeDetailState,
    ctaLabel: String = "참여하기",
    onIntent: (ChallengeDetailIntent) -> Unit = {},
    onBack: () -> Unit = {},
    onCta: () -> Unit = {},
) = setContent {
    RuleUpTheme {
        ChallengeDetailContent(
            state = state,
            ctaLabel = ctaLabel,
            onIntent = onIntent,
            onBack = onBack,
            onCta = onCta,
        )
    }
}

/** 바텀시트는 열리는 애니메이션이 끝나야 노드가 붙는다. `Thread.sleep` 대신 조건을 기다린다. */
internal fun ComposeContentTestRule.awaitText(text: String) {
    waitUntil { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
}
