package com.ruleup.profile.presentation.temperature

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.profile.domain.entity.ReputationDetail
import com.ruleup.profile.presentation.clickPastGuard
import com.ruleup.profile.presentation.renderScreen
import com.ruleup.profile.presentation.temperature.viewmodel.MyTemperatureIntent
import com.ruleup.profile.presentation.temperature.viewmodel.MyTemperatureState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

/**
 * 매너 온도 상세. 온도는 사용자가 자기 평판을 확인하는 값이라 **못 불러온 것과 낮은 것을 섞으면**
 * 하지도 않은 일로 점수가 깎인 줄 안다.
 */
@RunWith(RobolectricTestRunner::class)
class MyTemperatureContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `불러오는 중에는 실패 문구를 띄우지 않는다`() {
        render(MyTemperatureState.initial.copy(isLoading = true))

        compose.onNodeWithText("온도 정보를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `조회에 실패하면 사유를 보여 준다`() {
        render(MyTemperatureState.initial.copy(isLoading = false, errorMessage = "네트워크가 끊겼어요"))

        compose.onNodeWithText("네트워크가 끊겼어요").assertExists()
    }

    @Test
    fun `사유를 모르는 실패도 빈 화면으로 두지 않는다`() {
        render(MyTemperatureState.initial.copy(isLoading = false, errorMessage = null))

        compose.onNodeWithText("온도 정보를 불러오지 못했어요").assertExists()
    }

    @Test
    fun `온도를 받으면 실패 문구를 띄우지 않는다`() {
        render(MyTemperatureState.initial.copy(isLoading = false, detail = detail()))

        compose.onNodeWithText("온도 정보를 불러오지 못했어요").assertDoesNotExist()
    }

    @Test
    fun `뒤로 가기 의도가 올라간다`() {
        val intents = mutableListOf<MyTemperatureIntent>()
        render(MyTemperatureState.initial) { intents += it }

        compose.onNodeWithContentDescription("뒤로").clickPastGuard()

        assertTrue(intents.contains(MyTemperatureIntent.Back))
    }

    private fun detail() = ReputationDetail(current = 36.7, bandLabel = "따뜻해요", nextTier = null, recentChanges = emptyList())

    private fun render(
        state: MyTemperatureState,
        onIntent: (MyTemperatureIntent) -> Unit = {},
    ) {
        compose.renderScreen { MyTemperatureContent(state = state, onIntent = onIntent) }
    }
}
