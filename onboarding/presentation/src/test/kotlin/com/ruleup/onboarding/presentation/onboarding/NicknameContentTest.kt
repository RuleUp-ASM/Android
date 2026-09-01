package com.ruleup.onboarding.presentation.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ruleup.designsystem.SingleClickGuard
import com.ruleup.designsystem.theme.RuleUpTheme
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.observability.domain.test.testObservability
import com.ruleup.onboarding.presentation.onboarding.viewmodel.OnboardingIntent
import com.ruleup.ui.helper.LocalNavigationHelper
import com.ruleup.ui.helper.LocalObservability
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 닉네임 단계. **서버 확인을 통과해야만** 다음으로 넘어간다 — 통과 전에 전진시키면 마지막 제출에서
 * 1단계로 되돌아오고, signupToken 은 5분이라 그 왕복이 곧 가입 이탈이 된다.
 *
 * 잠금이 `enabled=false` 가 아니라 **클릭 조기 반환**이라 `assertIsNotEnabled` 로는 잡히지 않는다.
 * 눌러 보고 이동이 없었는지를 봐야 실제 계약을 검증한다.
 */
@RunWith(RobolectricTestRunner::class)
class NicknameContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `서버 확인 전에는 다음을 눌러도 넘어가지 않는다`() {
        val nav = render(nicknameAvailable = null)

        compose.onNodeWithText("다음").clickPastGuard()

        assertTrue(nav.didNotMove)
    }

    @Test
    fun `이미 쓰는 닉네임이면 다음을 눌러도 넘어가지 않는다`() {
        val nav = render(nicknameAvailable = false)

        compose.onNodeWithText("다음").clickPastGuard()

        assertTrue(nav.didNotMove)
    }

    @Test
    fun `확인을 통과하면 다음 단계로 넘어간다`() {
        val nav = render(nicknameAvailable = true)

        compose.onNodeWithText("다음").clickPastGuard()

        assertEquals(1, nav.pages.size)
    }

    @Test
    fun `1단계에서 뒤로 가면 이탈로 다룬다`() {
        // signupToken 만료로 되돌아올 수 없어, 일반 뒤로가기와 다르게 처리해야 한다.
        val intents = mutableListOf<OnboardingIntent>()
        render(nicknameAvailable = true, onIntent = { intents += it })

        compose.onNodeWithContentDescription("이전 단계로").clickPastGuard()

        assertTrue(intents.contains(OnboardingIntent.BackFromFirstStep))
    }

    @Test
    fun `입력한 닉네임은 그대로 의도로 올라간다`() {
        val intents = mutableListOf<OnboardingIntent>()
        render(onIntent = { intents += it })

        compose.onNodeWithText("닉네임을 입력하세요").performTextInput("지현")

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.SetNickName("지현")), intents)
    }

    private fun render(
        nicknameAvailable: Boolean? = null,
        nickname: String = "",
        onIntent: (OnboardingIntent) -> Unit = {},
    ): RecordingNavigationHelper {
        val nav = RecordingNavigationHelper()
        compose.setContent {
            RuleUpTheme {
                // 화면이 소비하는 CompositionLocal 은 테스트가 직접 채운다 — 값이 없으면
                // 렌더 전에 error() 로 죽어 무엇이 문제인지 화면 코드처럼 보인다.
                CompositionLocalProvider(
                    LocalNavigationHelper provides nav,
                    LocalObservability provides testObservability(),
                ) {
                    NicknameContent(
                        onIntent = onIntent,
                        nickname = nickname,
                        nicknameAvailable = nicknameAvailable,
                    )
                }
            }
        }
        return nav
    }
}

/**
 * 전역 [SingleClickGuard] 를 넘겨 누른다.
 *
 * 가드는 `SystemClock.elapsedRealtime()` 을 보는데 Robolectric 은 **테스트마다 시계를 되감는다.**
 * 반면 가드의 마지막 클릭 시각은 `object` 필드라 테스트를 건너 남는다. 그래서 매번 같은 양만
 * 전진시키면 두 번째 테스트부터 차이가 0 이하가 되어 클릭이 조용히 삼켜진다 — 하나만 돌리면
 * 통과하는데 클래스 전체를 돌리면 깨지는, 원인을 찾기 가장 어려운 형태다.
 *
 * 그래서 클릭할 때마다 **누적**해서 민다. 되감긴 시계에서도 직전 테스트가 남긴 값보다 항상 앞선다.
 */
private object ClickClock {
    private var elapsed = 0L

    fun advance() {
        elapsed += SingleClickGuard.DEFAULT_THROTTLE_MILLIS * 4
        ShadowSystemClock.advanceBy(Duration.ofMillis(elapsed))
    }
}

private fun SemanticsNodeInteraction.clickPastGuard() {
    ClickClock.advance()
    performClick()
}
