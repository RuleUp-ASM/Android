package com.ruleup.notification.presentation.center.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.notification.domain.entity.NotificationTab
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.notification.presentation.notification
import com.ruleup.notification.presentation.page
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
import kotlin.test.assertTrue

/**
 * 알림 센터.
 *
 * 읽음 처리가 이 화면의 함정이다. **보내는 값은 응답에 실제로 담겼던 최신 id** 여야 하고,
 * **첫 페이지에서만** 보내야 한다 — 현재 시각으로 갱신하면 조회와 갱신 사이에 적재된 알림이
 * 화면에 뜬 적 없이 읽음 처리되어 레드닷이 영영 안 뜨고, 뒤 페이지에서 보내면 더 작은 id 로
 * 읽음 지점이 과거로 밀린다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `진입하면 목록을 올리고 그 시점 최신 id 로 읽음 처리한다`() =
        runTest {
            val repo =
                FakeNotificationRepository(
                    page = { page(notification("n3"), notification("n2"), notification("n1")) },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)

            assertEquals(3, viewModel.uiState.value.items.size)
            assertEquals(NotificationTab.NOTIFICATION to "n3", repo.readMarkers.single())
        }

    @Test
    fun `목록이 비면 읽음 처리를 보내지 않는다`() =
        runTest {
            // 보낼 id 가 없다 — 서버가 NOTIFICATION_NOT_FOUND 로 막는 요청을 굳이 만들지 않는다.
            val repo = FakeNotificationRepository(page = { page() })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)

            assertTrue(repo.readMarkers.isEmpty())
        }

    @Test
    fun `기준선 위 항목만 미읽음으로 표시한다`() =
        runTest {
            // id 크기가 아니라 목록에서의 위치로 판단한다 — 최신순이라 위가 새것이다.
            val repo =
                FakeNotificationRepository(
                    page = {
                        page(
                            notification("n3"),
                            notification("n2"),
                            notification("n1"),
                            lastReadNotificationId = "n2",
                        )
                    },
                )
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)

            assertEquals(setOf("n3"), viewModel.uiState.value.unreadIds)
        }

    @Test
    fun `기준선이 없으면 전부 미읽음이다`() =
        runTest {
            val repo = FakeNotificationRepository(page = { page(notification("n2"), notification("n1")) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)

            assertEquals(setOf("n2", "n1"), viewModel.uiState.value.unreadIds)
        }

    @Test
    fun `다음 페이지를 불러올 때는 읽음 처리를 다시 보내지 않는다`() =
        runTest {
            // 뒤 페이지는 id 가 더 작아 읽음 지점이 과거로 밀린다.
            val repo =
                FakeNotificationRepository(
                    page = { cursor ->
                        if (cursor == null) {
                            page(notification("n3"), nextCursor = "c2")
                        } else {
                            page(notification("n1"))
                        }
                    },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(NotificationCenterIntent.Load)

            viewModel.onIntent(NotificationCenterIntent.LoadMore)

            assertEquals(listOf(null, "c2"), repo.cursors)
            assertEquals(1, repo.readMarkers.size)
            assertEquals(2, viewModel.uiState.value.items.size)
        }

    @Test
    fun `읽음 처리를 눌러도 미읽음 표시는 세션 내내 남는다`() =
        runTest {
            // 눈앞에서 점이 사라지면 무엇이 새로 온 것이었는지 알 수 없다.
            val repo = FakeNotificationRepository(page = { page(notification("n1")) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)

            assertEquals(setOf("n1"), viewModel.uiState.value.unreadIds)
            assertTrue(repo.readMarkers.isNotEmpty())
        }

    @Test
    fun `갈 곳이 없는 알림을 눌러도 아무 데도 보내지 않는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val repo = FakeNotificationRepository(page = { page(notification("n1", deeplink = null)) })
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(NotificationCenterIntent.Load)

            viewModel.onIntent(NotificationCenterIntent.Open(notification("n1", deeplink = null)))

            assertTrue(nav.didNotMove)
        }

    @Test
    fun `조회에 실패하면 사유를 남긴다`() =
        runTest {
            val repo = FakeNotificationRepository(page = { throw IllegalStateException("서버 오류") })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)

            assertEquals("서버 오류", viewModel.uiState.value.errorMessage)
        }

    @Test
    fun `공지 탭으로 옮기면 공지만 묻는다`() =
        runTest {
            // 공지는 별도 API 가 아니라 같은 목록의 tab 필터다(2026-09-07 확정).
            val repo = FakeNotificationRepository(page = { page(notification("n1")) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)
            viewModel.onIntent(NotificationCenterIntent.SelectTab(NotificationTab.ANNOUNCEMENT))

            assertEquals(
                listOf(NotificationTab.NOTIFICATION, NotificationTab.ANNOUNCEMENT),
                repo.tabs,
            )
            assertEquals(NotificationTab.ANNOUNCEMENT, viewModel.uiState.value.tab)
        }

    @Test
    fun `읽음 처리는 탭마다 한 번씩만 보낸다`() =
        runTest {
            // 읽음 지점이 탭별로 따로 보관되므로 탭마다 필요하고, 오갈 때마다 다시 보낼 이유는 없다.
            val repo = FakeNotificationRepository(page = { page(notification("n1")) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)
            viewModel.onIntent(NotificationCenterIntent.SelectTab(NotificationTab.ANNOUNCEMENT))
            viewModel.onIntent(NotificationCenterIntent.SelectTab(NotificationTab.NOTIFICATION))
            viewModel.onIntent(NotificationCenterIntent.SelectTab(NotificationTab.ANNOUNCEMENT))

            assertEquals(
                listOf(NotificationTab.NOTIFICATION to "n1", NotificationTab.ANNOUNCEMENT to "n1"),
                repo.readMarkers,
            )
        }

    @Test
    fun `같은 탭을 다시 눌러도 다시 묻지 않는다`() =
        runTest {
            val repo = FakeNotificationRepository(page = { page(notification("n1")) })
            val viewModel = viewModel(repo)

            viewModel.onIntent(NotificationCenterIntent.Load)
            viewModel.onIntent(NotificationCenterIntent.SelectTab(NotificationTab.NOTIFICATION))

            assertEquals(1, repo.calls.count { it == "getNotifications" })
        }

    private fun viewModel(
        repo: FakeNotificationRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = NotificationCenterViewModel(notificationRepository = repo, navigationHelper = nav)
}
