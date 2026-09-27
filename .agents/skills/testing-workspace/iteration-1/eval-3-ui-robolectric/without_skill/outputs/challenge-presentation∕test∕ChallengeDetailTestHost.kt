package com.ruleup.challenge.presentation.detail

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailIntent
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.designsystem.theme.RuleUpTheme
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

/**
 * 상세 화면을 ViewModel 없이 그린다.
 *
 * `ChallengeDetailScreen` 은 `hiltViewModel()` 로 ViewModel 을 스스로 만들고 진입 시 Load 인텐트를 쏘므로
 * 화면째로 그리면 네트워크·DI 를 세우게 된다. 상태별 렌더는 상태를 인자로 받는 조립부에 다 들어 있어
 * 여기만 그리면 충분하다 — 대신 CTA 라벨을 고르는 계산(setup·권한 → DetailSetupAction)은 이 경계 밖이라
 * 라벨을 완성된 값으로 넘긴다.
 */
internal fun ComposeContentTestRule.showDetail(
    state: ChallengeDetailState,
    ctaLabel: String = "참여하기",
    onIntent: (ChallengeDetailIntent) -> Unit = {},
    onBack: () -> Unit = {},
    onCta: () -> Unit = {},
) {
    setContent {
        // colors 는 staticCompositionLocalOf 라 테마를 안 씌우면 렌더가 예외로 죽는다.
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
}

/** 방출된 인텐트를 순서대로 담는다. 클릭이 무엇을 올려보냈는지가 화면의 계약이다. */
internal class RecordedIntents {
    private val recorded = mutableListOf<ChallengeDetailIntent>()

    val all: List<ChallengeDetailIntent> get() = recorded

    val last: ChallengeDetailIntent? get() = recorded.lastOrNull()

    fun record(intent: ChallengeDetailIntent) {
        recorded += intent
    }
}

/**
 * `singleClickable` 은 전역 연타 가드(300ms)를 `SystemClock.elapsedRealtime()` 으로 판정한다.
 * Robolectric 의 시계는 멈춰 있어 그냥 두면 **첫 클릭부터** 가드에 먹힌다 — 클릭 전에 시계를 민다.
 */
internal fun SemanticsNodeInteraction.performGuardedClick(): SemanticsNodeInteraction {
    ShadowSystemClock.advanceBy(Duration.ofSeconds(1))
    return performClick()
}

internal fun ComposeContentTestRule.clickText(text: String) {
    onNodeWithText(text).performGuardedClick()
}

internal fun ComposeContentTestRule.clickDescription(description: String) {
    onNodeWithContentDescription(description).performGuardedClick()
}
