package com.ruleup.notification.presentation.settings.viewmodel

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

/**
 * 알림 설정.
 *
 * 두 가지가 계약이다. **보낸 필드만 보낸다** — 서버가 허용되지 않은 키를 400 으로 막으므로 빈
 * 그룹 객체를 실어 보내면 안 된다. 그리고 **낙관적 반영을 하지 않는다** — 마케팅 토글은 수신
 * 동의를 같은 트랜잭션에서 바꾸고 그 시각이 법적 기록이다.
 */
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
    fun `서버가 받아들인 값으로만 화면을 갱신한다`() =
        runTest {
            // 낙관적 반영을 하면 실패했을 때 화면이 거짓말한다.
            val repo =
                FakeNotificationRepository(
                    settings = { settings() },
                    update = { NotificationSettingsResult(settings(challenge = false), marketingConsentSyncedAt = null) },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleGroup(NotificationGroup.CHALLENGE, false))

            assertEquals(
                false,
                viewModel.uiState.value.settings
                    ?.groups
                    ?.challenge,
            )
        }

    @Test
    fun `변경에 실패하면 사유를 알리고 값을 바꾸지 않는다`() =
        runTest {
            val repo =
                FakeNotificationRepository(
                    settings = { settings() },
                    update = { throw IllegalStateException("설정 저장 실패") },
                )
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)
            viewModel.onIntent(NotificationSettingsIntent.Load)

            viewModel.onIntent(NotificationSettingsIntent.ToggleGroup(NotificationGroup.CHALLENGE, false))

            assertEquals(
                true,
                viewModel.uiState.value.settings
                    ?.groups
                    ?.challenge,
            )
            assertTrue(effects.filterIsInstance<NotificationSettingsEffect.ShowMessage>().isNotEmpty())
        }

    @Test
    fun `OS 권한 상태는 서버 설정값을 건드리지 않는다`() =
        runTest {
            // 권한을 거부해도 서버 값은 그대로다(정책 §3.1) — 배너만 뜬다.
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
    ) = NotificationSettingsViewModel(notificationRepository = repo, navigationHelper = nav)
}
