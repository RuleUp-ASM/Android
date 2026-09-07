package com.ruleup.challenge.presentation.watcher.viewmodel

import com.ruleup.challenge.domain.entity.AlreadyConsentedException
import com.ruleup.challenge.domain.entity.CannotWatchSelfException
import com.ruleup.challenge.domain.entity.InvitationExpiredException
import com.ruleup.challenge.domain.entity.WatcherAcceptance
import com.ruleup.challenge.domain.entity.WatcherChannel
import com.ruleup.challenge.domain.entity.WatcherStatus
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 감시자 초대 수락. **수락이 곧 개인정보 수신 동의**라, 링크를 연 것만으로 동의가 성립하면 안 된다 —
 * 화면 진입과 수락 요청이 분리돼 있는지가 이 화면의 계약이다.
 *
 * 실패 사유마다 다음에 할 일이 다르므로(재초대 요청 · 이미 됨 · 안 되는 일) 하나로 접지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WatcherAcceptViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `화면에 들어온 것만으로는 수락하지 않는다`() =
        runTest {
            // 링크를 눌러 본 것과 동의한 것은 다르다.
            val repo = FakeWatcherRepository(accept = { acceptance() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(WatcherAcceptIntent.Load("wtk_1"))

            assertTrue(repo.acceptedTokens.isEmpty())
            assertNull(viewModel.uiState.value.accepted)
        }

    @Test
    fun `수락하면 링크에서 받은 토큰을 그대로 보낸다`() =
        runTest {
            val repo = FakeWatcherRepository(accept = { acceptance() })
            val viewModel = viewModel(repo)
            viewModel.onIntent(WatcherAcceptIntent.Load("wtk_8f3a"))

            viewModel.onIntent(WatcherAcceptIntent.Accept)

            assertEquals(listOf("wtk_8f3a"), repo.acceptedTokens)
            assertEquals(
                WatcherStatus.ACTIVE,
                viewModel.uiState.value.accepted
                    ?.status,
            )
        }

    @Test
    fun `토큰이 비어 있으면 요청하지 않고 링크 오류로 알린다`() =
        runTest {
            val repo = FakeWatcherRepository(accept = { acceptance() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(WatcherAcceptIntent.Load(""))
            viewModel.onIntent(WatcherAcceptIntent.Accept)

            assertEquals(WatcherAcceptFailure.INVALID, viewModel.uiState.value.failure)
            assertTrue(repo.acceptedTokens.isEmpty())
        }

    @Test
    fun `만료된 초대는 다시 초대해 달라고 안내할 수 있게 구분한다`() =
        runTest {
            val viewModel = viewModel(FakeWatcherRepository(accept = { throw InvitationExpiredException() }))
            viewModel.onIntent(WatcherAcceptIntent.Load("wtk_1"))

            viewModel.onIntent(WatcherAcceptIntent.Accept)

            assertEquals(WatcherAcceptFailure.EXPIRED, viewModel.uiState.value.failure)
        }

    @Test
    fun `이미 수락한 초대는 실패가 아니라 이미 됐다고 구분한다`() =
        runTest {
            val viewModel = viewModel(FakeWatcherRepository(accept = { throw AlreadyConsentedException() }))
            viewModel.onIntent(WatcherAcceptIntent.Load("wtk_1"))

            viewModel.onIntent(WatcherAcceptIntent.Accept)

            assertEquals(WatcherAcceptFailure.ALREADY_ACCEPTED, viewModel.uiState.value.failure)
        }

    @Test
    fun `본인 초대 수락은 안 되는 일이라고 구분한다`() =
        runTest {
            val viewModel = viewModel(FakeWatcherRepository(accept = { throw CannotWatchSelfException() }))
            viewModel.onIntent(WatcherAcceptIntent.Load("wtk_1"))

            viewModel.onIntent(WatcherAcceptIntent.Accept)

            assertEquals(WatcherAcceptFailure.SELF, viewModel.uiState.value.failure)
        }

    private fun viewModel(
        repo: FakeWatcherRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = WatcherAcceptViewModel(watcherRepository = repo, navigationHelper = nav)

    private fun acceptance() =
        WatcherAcceptance(
            watcherId = "w1",
            status = WatcherStatus.ACTIVE,
            channel = WatcherChannel.IN_APP,
        )
}
