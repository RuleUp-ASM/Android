package com.ruleup.onboarding.presentation.walkthrough.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.onboarding.domain.fake.FakeWalkthroughRepository
import com.ruleup.onboarding.domain.navigation.LoginPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 워크쓰루를 끝내는 길이 둘(마지막 장 CTA·건너뛰기)인데 **둘 다 열람 기록을 남겨야 한다** —
 * 한쪽이 빠지면 앱을 열 때마다 소개가 다시 뜬다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WalkthroughViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `다음을 누르면 마지막 장까지 한 장씩 넘어간다`() =
        runTest {
            val viewModel = viewModel()

            viewModel.onIntent(WalkthroughIntent.Next)
            assertEquals(WalkthroughPageIndex.KEEP, viewModel.uiState.value.page)

            viewModel.onIntent(WalkthroughIntent.Next)
            assertEquals(WalkthroughPageIndex.STAY, viewModel.uiState.value.page)
        }

    @Test
    fun `마지막 장이 아니면 끝내지 않는다`() =
        runTest {
            // 중간에 이동해 버리면 남은 장을 영영 못 본다 — 열람 기록도 그때 남으면 안 된다.
            val walkthrough = FakeWalkthroughRepository()
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(walkthrough, nav)

            viewModel.onIntent(WalkthroughIntent.Next)

            assertEquals(0, walkthrough.markSeenCount)
            assertEquals(0, nav.pages.size)
        }

    @Test
    fun `마지막 장에서 시작하기를 누르면 본 것으로 기록하고 로그인으로 간다`() =
        runTest {
            val walkthrough = FakeWalkthroughRepository()
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(walkthrough, nav)

            repeat(WalkthroughPageIndex.entries.size) { viewModel.onIntent(WalkthroughIntent.Next) }

            assertEquals(1, walkthrough.markSeenCount)
            assertEquals(
                LoginPage.PATH,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    @Test
    fun `건너뛰기도 본 것으로 기록한다`() =
        runTest {
            // 기록을 빠뜨리면 다음 실행에서 소개가 다시 뜬다 — 건너뛴 사용자에게는 더 성가시다.
            val walkthrough = FakeWalkthroughRepository()
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(walkthrough, nav)

            viewModel.onIntent(WalkthroughIntent.Skip)

            assertEquals(1, walkthrough.markSeenCount)
            assertEquals(
                LoginPage.PATH,
                nav.pages
                    .single()
                    .toRoute()
                    .path,
            )
        }

    private fun viewModel(
        walkthrough: FakeWalkthroughRepository = FakeWalkthroughRepository(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = WalkthroughViewModel(walkthroughRepository = walkthrough, navigationHelper = nav)
}
