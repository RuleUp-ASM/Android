package com.ruleup.challenge.presentation.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.renderScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/** 챌린지 사진 고르기·빼기(#562). 마이페이지 프로필 사진과 같은 방식이다. */
@RunWith(RobolectricTestRunner::class)
class CoverImagePickerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `사진이 없으면 빈 칸을 눌러 고르게 하고 빼기 버튼은 없다`() {
        val calls = mutableListOf<String>()
        render(image = null, enabled = true, calls = calls)

        compose.onNodeWithText("사진 고르기").clickPastGuard()

        assertEquals(listOf("pick"), calls)
        compose.onNodeWithContentDescription("사진 제거").assertDoesNotExist()
    }

    @Test
    fun `사진이 있으면 모서리 X 로 뺀다`() {
        val calls = mutableListOf<String>()
        render(image = "https://example.com/cover.jpg", enabled = true, calls = calls)

        compose.onNodeWithContentDescription("사진 제거").clickPastGuard()

        assertEquals(listOf("remove"), calls)
    }

    @Test
    fun `바꿀 수 없는 상태면 사진이 있어도 빼기 버튼을 그리지 않는다`() {
        // 심사 중·만드는 중에 빼기가 보이면 눌러도 반영되지 않아 고장처럼 보인다.
        render(image = "https://example.com/cover.jpg", enabled = false, calls = mutableListOf())

        compose.onNodeWithContentDescription("사진 제거").assertDoesNotExist()
    }

    private fun render(
        image: String?,
        enabled: Boolean,
        calls: MutableList<String>,
    ) {
        compose.renderScreen {
            CoverImagePicker(image = image, enabled = enabled, onPick = { calls += "pick" }, onRemove = { calls += "remove" })
        }
    }
}
