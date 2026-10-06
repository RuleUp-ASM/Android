package com.ruleup.challenge.presentation.detail.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ruleup.challenge.domain.entity.ThreadItem
import com.ruleup.challenge.domain.entity.ThreadItemType
import com.ruleup.challenge.presentation.clickPastGuard
import com.ruleup.challenge.presentation.detail.viewmodel.ChallengeDetailState
import com.ruleup.challenge.presentation.renderScreen
import com.ruleup.domain.entity.user.User
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 방 피드. */
@RunWith(RobolectricTestRunner::class)
class RoomFeedTabTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `다른 사람의 피드 글을 누르면 그 작성자 프로필로 간다`() {
        // 누가 인증했는지 보고 그 사람을 알아보는 동선이다(#582).
        val opened = mutableListOf<String>()
        render(item("v1", User(id = "u2", nickname = "서연", profileImageUrl = null))) { opened += it }

        compose.onNodeWithText("서연").clickPastGuard()

        assertEquals(listOf("u2"), opened)
    }

    @Test
    fun `내 피드 글은 눌러도 프로필로 가지 않는다`() {
        // 내 프로필은 마이페이지가 원본이다 — 멤버 목록과 같은 규칙.
        val opened = mutableListOf<String>()
        render(item("v1", User(id = "me", nickname = "나무", profileImageUrl = null))) { opened += it }

        compose.onNodeWithText("나무 (나)").clickPastGuard()

        assertTrue(opened.isEmpty())
    }

    private fun item(
        id: String,
        user: User,
    ) = ThreadItem(
        type = ThreadItemType.VERIFY_SUCCESS,
        id = id,
        user = user,
        at = "2026-10-06T06:12:00+09:00",
        streak = 3,
        failDate = null,
    )

    private fun render(
        vararg items: ThreadItem,
        onOpenProfile: (String) -> Unit,
    ) {
        compose.renderScreen {
            RoomFeedTab(
                state = ChallengeDetailState.initial.copy(isLoading = false, threads = items.toList(), myUserId = "me"),
                onLoadMore = {},
                onRetry = {},
                onOpenProfile = onOpenProfile,
            )
        }
    }
}
