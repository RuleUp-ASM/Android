package com.ruleup.profile.presentation.watching.viewmodel

import com.ruleup.challenge.domain.entity.AlreadyRevokedException
import com.ruleup.challenge.domain.entity.WatcherStatus
import com.ruleup.challenge.domain.entity.Watching
import com.ruleup.challenge.domain.entity.WatchingUpdate
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
 * 패널티 수신 관리. 두 동작이 **결과가 다르다** — 푸시 토글은 관계를 남기고, 완전 수신거부는
 * 되돌릴 수 없으며 30일 재초대 차단까지 건다. 서버가 둘의 동시 전송을 막으므로 요청도 하나씩이다.
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

    @Test
    fun `푸시만 끌 때는 수신거부를 함께 보내지 않는다`() =
        runTest {
            // 서버가 pushEnabled 와 revoke 동시 전송을 400 으로 막는다.
            val repo =
                FakeWatcherRepository(
                    watching = { listOf(watching("w1")) },
                    update = { id, push, _ -> update(id, pushEnabled = push ?: true) },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(WatchingIntent.Load)

            viewModel.onIntent(WatchingIntent.TogglePush("w1", false))

            assertEquals(Triple("w1", false, null), repo.updateArgs.single())
        }

    @Test
    fun `푸시를 끄면 그 행만 서버가 준 값으로 바꾼다`() =
        runTest {
            val repo =
                FakeWatcherRepository(
                    watching = { listOf(watching("w1"), watching("w2")) },
                    update = { id, push, _ -> update(id, pushEnabled = push ?: true) },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(WatchingIntent.Load)

            viewModel.onIntent(WatchingIntent.TogglePush("w1", false))

            assertEquals(
                false,
                viewModel.uiState.value.items
                    .first { it.watcherId == "w1" }
                    .pushEnabled,
            )
            assertEquals(
                true,
                viewModel.uiState.value.items
                    .first { it.watcherId == "w2" }
                    .pushEnabled,
            )
            assertEquals(1, repo.calls.count { it == "getWatching" })
        }

    @Test
    fun `완전 수신거부는 확인을 거쳐야 나간다`() =
        runTest {
            // 되돌릴 수 없고 30일 재초대 차단까지 걸리므로 한 번 묻는다.
            val repo = FakeWatcherRepository(watching = { listOf(watching("w1")) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(WatchingIntent.Load)

            viewModel.onIntent(WatchingIntent.ConfirmRevoke("w1"))

            assertEquals("w1", viewModel.uiState.value.revokeTarget)
            assertTrue(repo.updateArgs.isEmpty())
        }

    @Test
    fun `수신을 거부하면 목록에서 빼고 시트를 닫는다`() =
        runTest {
            val repo =
                FakeWatcherRepository(
                    watching = { listOf(watching("w1")) },
                    update = { id, _, _ -> update(id, pushEnabled = false, status = WatcherStatus.REVOKED) },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(WatchingIntent.Load)
            viewModel.onIntent(WatchingIntent.ConfirmRevoke("w1"))

            viewModel.onIntent(WatchingIntent.Revoke)

            assertEquals(Triple("w1", null, true), repo.updateArgs.single())
            assertTrue(
                viewModel.uiState.value.items
                    .isEmpty(),
            )
            assertNull(viewModel.uiState.value.revokeTarget)
        }

    @Test
    fun `이미 거부된 항목이면 실패로 알리지 않고 목록에서 뺀다`() =
        runTest {
            // 사용자가 원한 결과가 이미 이뤄진 상태다 — 오류를 띄우면 뭘 더 해야 하는지 알 수 없다.
            val repo =
                FakeWatcherRepository(
                    watching = { listOf(watching("w1")) },
                    update = { _, _, _ -> throw AlreadyRevokedException() },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(WatchingIntent.Load)
            viewModel.onIntent(WatchingIntent.ConfirmRevoke("w1"))

            viewModel.onIntent(WatchingIntent.Revoke)

            assertTrue(
                viewModel.uiState.value.items
                    .isEmpty(),
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

    private fun update(
        id: String,
        pushEnabled: Boolean,
        status: WatcherStatus = WatcherStatus.ACTIVE,
    ) = WatchingUpdate(
        watcherId = id,
        status = status,
        pushEnabled = pushEnabled,
        inboxKept = true,
        reblockUntil = null,
    )
}
