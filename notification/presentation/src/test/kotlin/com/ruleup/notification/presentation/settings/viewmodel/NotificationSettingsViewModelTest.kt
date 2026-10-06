package com.ruleup.notification.presentation.settings.viewmodel

import com.ruleup.domain.test.RecordingMessageHelper
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.notification.domain.entity.NotificationGroup
import com.ruleup.notification.domain.entity.NotificationSettingsResult
import com.ruleup.notification.domain.fake.FakeNotificationRepository
import com.ruleup.notification.presentation.settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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

/** 알림 설정. */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationSettingsViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 설정을 화면에 올린다`() =
        runTest {
            val viewModel = viewModel(FakeNotificationRepository(settings = { settings(marketing = false) }))

            viewModel.onIntent(NotificationSettingsIntent.Load)

            assertEquals(
                false,
                viewModel.uiState.value.settings
                    ?.groups
                    ?.marketing,
            )
        }

    @Test
    fun `그룹 하나를 끄면 그 필드만 실어 보낸다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleGroup(NotificationGroup.CHALLENGE, false))

            val sent = repo.updates.single()
            assertEquals(false, sent.challenge)
            assertNull(sent.account)
            assertNull(sent.marketing)
            assertNull(sent.pushEnabled)
        }

    @Test
    fun `마스터를 끄면 그룹 필드는 보내지 않는다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleMaster(false))

            val sent = repo.updates.single()
            assertEquals(false, sent.pushEnabled)
            assertNull(sent.challenge)
        }

    @Test
    fun `마케팅을 끄면 수신 동의도 철회된다는 걸 알린다`() =
        runTest {
            // "알림만 끄는 것"으로 읽히면 사용자가 동의 철회를 모른 채 지나간다.
            val viewModel = viewModel(repo())
            val effects = collectEffects(viewModel)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleGroup(NotificationGroup.MARKETING, false))

            assertTrue(
                effects.filterIsInstance<NotificationSettingsEffect.ShowMessage>().any {
                    it.message.contains("철회")
                },
            )
        }

    @Test
    fun `토글하면 응답을 기다리지 않고 화면부터 바꾼다`() =
        runTest {
            // 응답이 올 때까지 스위치가 제자리면 눌리지 않은 줄 알고 다시 누른다(#564).
            lateinit var viewModel: NotificationSettingsViewModel
            var shownWhileSending: Boolean? = null
            val repo =
                FakeNotificationRepository(
                    settings = { settings() },
                    update = {
                        shownWhileSending =
                            viewModel.uiState.value.settings
                                ?.groups
                                ?.challenge
                        NotificationSettingsResult(settings(challenge = false), marketingConsentSyncedAt = null)
                    },
                )
            viewModel = viewModel(repo)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleGroup(NotificationGroup.CHALLENGE, false))

            assertEquals(false, shownWhileSending)
        }

    @Test
    fun `변경에 실패하면 요청 전 값으로 되돌리고 모달로 알린다`() =
        runTest {
            // 되돌리지 않으면 서버는 그대로인데 화면만 바뀐 채 남는다.
            val repo =
                FakeNotificationRepository(
                    settings = { settings() },
                    update = { throw IllegalStateException("설정 저장 실패") },
                )
            val messages = RecordingMessageHelper()
            val viewModel = viewModel(repo, messages = messages)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleGroup(NotificationGroup.CHALLENGE, false))

            assertEquals(
                true,
                viewModel.uiState.value.settings
                    ?.groups
                    ?.challenge,
            )
            assertEquals(1, messages.dialogDescriptions.size)
        }

    @Test
    fun `OS 권한 상태는 서버 설정값을 건드리지 않는다`() =
        runTest {
            // 권한을 거부해도 서버 값은 그대로다
            val viewModel = viewModel(repo())
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onPermissionChecked(denied = true)

            assertTrue(viewModel.uiState.value.systemPermissionDenied)
            assertEquals(
                true,
                viewModel.uiState.value.settings
                    ?.pushEnabled,
            )
        }

    private fun repo() =
        FakeNotificationRepository(
            settings = { settings() },
            update = { NotificationSettingsResult(settings(), marketingConsentSyncedAt = null) },
        )

    private fun TestScope.collectEffects(viewModel: NotificationSettingsViewModel): List<NotificationSettingsEffect> {
        val effects = mutableListOf<NotificationSettingsEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun viewModel(
        repo: FakeNotificationRepository,
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
        messages: RecordingMessageHelper = RecordingMessageHelper(),
    ) = NotificationSettingsViewModel(notificationRepository = repo, navigationHelper = nav, messageHelper = messages)
}
