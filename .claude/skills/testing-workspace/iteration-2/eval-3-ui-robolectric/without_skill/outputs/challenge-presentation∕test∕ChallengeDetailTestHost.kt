package com.ruleup.challenge.presentation.detail

import android.os.SystemClock
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.designsystem.theme.RuleUpTheme
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * 화면이 올려보낸 의도의 기록. 테스트는 상태를 넣고 **화면이 무엇을 그렸는지**와 **무엇을 올려보냈는지**
 * 둘만 본다 — ViewModel 은 이 층의 관심사가 아니다.
 */
internal class DetailHarness {
    val intents = mutableListOf<ChallengeDetailIntent>()
    var ctaClicks = 0
        private set
    var backClicks = 0
        private set

    internal fun recordCta() {
        ctaClicks++
    }

    internal fun recordBack() {
        backClicks++
    }

    /** 마지막으로 올라온 [T] 의도. 없으면 실패 메시지가 무엇이 올라왔는지 같이 알려준다. */
    inline fun <reified T : ChallengeDetailIntent> last(): T =
        intents.filterIsInstance<T>().lastOrNull()
            ?: error("${T::class.simpleName} 이 올라오지 않았다. 올라온 것: $intents")
}

/**
 * 상태 하나를 화면에 꽂는다. `ChallengeDetailScreen` 이 아니라 상태만 받는 [ChallengeDetailContent] 를
 * 그리는 이유는, 스크린이 `hiltViewModel()` 로 ViewModel 을 직접 만들기 때문이다 — 상태 조합마다
 * 가짜 ViewModel 을 세우면 이 층이 검증하려는 "상태 → 그림"이 DI 설정에 가려진다.
 */
internal fun ComposeContentTestRule.showDetail(
    state: ChallengeDetailState,
    ctaLabel: String = "참여하기",
): DetailHarness {
    val harness = DetailHarness()
    setContent {
        RuleUpTheme {
            ChallengeDetailContent(
                state = state,
                ctaLabel = ctaLabel,
                onIntent = { harness.intents += it },
                onBack = harness::recordBack,
                onCta = harness::recordCta,
            )
        }
    }
    return harness
}

/**
 * 클릭 가드를 통과시키고 누른다.
 *
 * `singleClickable` 은 `SystemClock.elapsedRealtime()` 로 연타를 막는데 Robolectric 의 시계는 멈춰 있어서
 * 그냥 두면 **첫 클릭부터** 전역 가드(300ms)에 먹힌다. 게다가 가드가 기억하는 마지막 클릭 시각은
 * static 이라 테스트 경계를 넘어 살아남는다 — 시계만 0 으로 되돌아가면 다음 테스트의 첫 클릭이
 * "과거"로 읽혀 또 막힌다. 그래서 되돌아가지 않는 커서를 두고 매 클릭 전에 그 앞으로 시계를 민다.
 */
internal fun ComposeContentTestRule.clickText(
    text: String,
    scroll: Boolean = false,
) {
    passClickGuard()
    val node = onNodeWithText(text)
    // 스크롤 컨테이너 안에 있는 대상만 민다 — 하단 고정 CTA 에 쓰면 "스크롤할 수 없다"로 깨진다.
    if (scroll) node.performScrollTo()
    node.performClick()
    waitForIdle()
}

internal fun ComposeContentTestRule.clickDescribed(description: String) {
    passClickGuard()
    onNodeWithContentDescription(description).performClick()
    waitForIdle()
}

/**
 * 시트·다이얼로그·드롭다운은 애니메이션이 끝나야 붙는다. 등장까지 기다린 뒤 노드를 돌려준다 —
 * `waitForIdle` 만으로는 프레임을 진행시키지 못해 "없다"로 잘못 실패한다.
 */
internal fun ComposeContentTestRule.awaitText(text: String): SemanticsNodeInteraction {
    waitUntil(WAIT_TIMEOUT_MILLIS) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
    return onNodeWithText(text)
}

internal fun ComposeContentTestRule.clickAwaited(text: String) {
    awaitText(text)
    clickText(text)
}

private const val WAIT_TIMEOUT_MILLIS = 5_000L
private const val CLICK_GUARD_STEP_MILLIS = 1_000L

private var clickCursorMillis = 10_000L

private fun passClickGuard() {
    clickCursorMillis += CLICK_GUARD_STEP_MILLIS
    val delta = clickCursorMillis - SystemClock.elapsedRealtime()
    if (delta > 0) ShadowSystemClock.advanceBy(Duration.ofMillis(delta))
}
