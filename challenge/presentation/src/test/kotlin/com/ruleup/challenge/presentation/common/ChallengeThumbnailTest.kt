package com.ruleup.challenge.presentation.common

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.designsystem.category.categoryEmoji
import com.ruleup.domain.entity.category.Category
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 둘러보기 목록 썸네일(#562). 사진이 없으면 상세와 같은 카테고리 타일이어야 같은 챌린지로 알아본다. */
@RunWith(RobolectricTestRunner::class)
class ChallengeThumbnailTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `대표 사진이 없으면 상세와 같은 카테고리 이모지 타일을 그린다`() {
        val category = Category.entries.first()
        compose.renderScreen {
            ChallengeThumbnail(imageUrl = null, category = category, size = 48.dp, cornerRadius = 12.dp, emojiSize = 22.sp)
        }

        compose.onNodeWithText(categoryEmoji(category)).assertExists()
    }

    @Test
    fun `카테고리를 모르면 기본 타일로 그린다`() {
        compose.renderScreen {
            ChallengeThumbnail(imageUrl = "", category = null, size = 48.dp, cornerRadius = 12.dp, emojiSize = 22.sp)
        }

        compose.onNodeWithText("🎯").assertExists()
    }
}
