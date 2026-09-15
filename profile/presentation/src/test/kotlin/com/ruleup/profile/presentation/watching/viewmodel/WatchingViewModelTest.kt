package com.ruleup.profile.presentation.watching.viewmodel

import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.challenge.domain.fake.FakeWatcherRepository
import com.ruleup.domain.test.RecordingNavigationHelper
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
 * 「내가 받는 알림」은 조회 전용이다. 여기서 지키는 건 **더는 알림이 오지 않는 관계를 세우지 않는 것**이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatchingViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `이미 거부한 관계는 목록에 세우지 않는다`() =
        runTest {
            // 되돌릴 방법이 없는데 토글을 보여 주면 다시 켤 수 있는 것처럼 보인다.
            val viewModel =
                viewModel(
                    FakeWatcherRepository(
                        watching = {
                            listOf(watching("w1"), watching("w2", status = WatcherStatus.REVOKED))
                        },
                    ),
                )

            viewModel.onIntent(WatchingIntent.Load)

            assertEquals(
                listOf("w1"),
                viewModel.uiState.value.items
                    .map { it.watcherId },
            )
        }

    private fun viewModel(
        repo: FakeWatcherRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = WatchingViewModel(watcherRepository = repo, navigationHelper = nav)

    private fun watching(
        id: String,
        status: WatcherStatus = WatcherStatus.ACTIVE,
    ) = Watching(
        watcherId = id,
        challengeTitle = "아침 6:30 기상",
        ownerNickname = "수민",
        status = status,
        pushEnabled = true,
        consentAt = "2026-07-20T11:00:00+09:00",
    )
}
