package com.ruleup.challenge.presentation.detail

import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.ui.helper.LocalMessageHelper
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * 화면을 세운다. `:app` 이 하는 일 중 이 화면이 실제로 요구하는 것만 흉내 낸다 —
 * 테마와 [LocalMessageHelper] 뿐이다(둘 다 없으면 컴포지션이 즉시 터진다).
 */
internal fun ComposeContentTestRule.showChallengeDetail(env: ChallengeDetailEnv) {
    setContent {
        RuleUpTheme {
            CompositionLocalProvider(LocalMessageHelper provides env.messages) {
                ChallengeDetailScreen(
                    challengeId = TEST_CHALLENGE_ID,
                    viewModel = env.viewModel,
                )
            }
        }
    }
    waitForIdle()
}

/**
 * 지금까지 이 JVM 에서 밀어 온 시계의 하한. 테스트마다 늘리기만 한다.
 *
 * 디자인 시스템의 클릭은 전부 `singleClickable` 이고 그 뒤에는 **프로세스 전역** 연타 가드가 있다.
 * Robolectric 은 테스트마다 `elapsedRealtime` 을 0 근처로 되돌리지만 가드가 기억하는 "마지막 클릭
 * 시각"은 그대로 남으므로, 시계를 되돌린 채 누르면 두 번째 테스트부터 클릭이 조용히 삼켜진다.
 */
private var clockFloorMillis = 0L

/** 연타 가드 창을 연 뒤 문구로 누른다. 화면의 클릭은 전부 이 경로로 보낸다. */
internal fun ComposeContentTestRule.tapText(text: String) {
    openClickWindow()
    onNodeWithText(text).performClick()
    waitForIdle()
}

/** 아이콘 버튼(뒤로·더 보기)은 contentDescription 으로 누른다. */
internal fun ComposeContentTestRule.tapIcon(contentDescription: String) {
    openClickWindow()
    onNodeWithContentDescription(contentDescription).performClick()
    waitForIdle()
}

private fun ComposeContentTestRule.openClickWindow() {
    clockFloorMillis += CLICK_WINDOW_MILLIS
    val now = SystemClock.elapsedRealtime()
    if (now < clockFloorMillis) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(clockFloorMillis - now))
    }
    waitForIdle()
}

// 전역 가드는 300ms, 위젯 가드는 500ms. 넉넉하되 프레임을 과하게 돌리지 않을 만큼만 민다.
private const val CLICK_WINDOW_MILLIS = 2_000L
