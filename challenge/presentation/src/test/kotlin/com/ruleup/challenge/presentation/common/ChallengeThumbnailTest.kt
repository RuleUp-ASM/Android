package com.ruleup.challenge.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.ruleup.challenge.presentation.detail.component.ChallengeCoverImage
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.domain.entity.category.Category
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 사진 없는 챌린지의 기본 커버(#575). 커버 그림은 SVG 에서 옮긴 벡터라 경로 문법이 하나만 틀려도
 * 그 카테고리 화면을 여는 순간 앱이 죽는다 — 여기서 먼저 걸리게 모든 카테고리를 한 번씩 그린다.
 */
@RunWith(RobolectricTestRunner::class)
class ChallengeThumbnailTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `사진이 없으면 모든 카테고리가 목록 썸네일과 상세 표지에 기본 커버를 그린다`() {
        compose.renderScreen {
            Column {
                (Category.entries + null).forEach { category ->
                    ChallengeThumbnail(imageUrl = null, category = category, size = 48.dp, cornerRadius = 12.dp)
                    ChallengeCoverImage(imageUrl = "", category = category, modifier = Modifier.size(360.dp, 800.dp))
                }
            }
        }

        compose.onRoot().assertExists()
    }
}
