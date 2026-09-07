package com.ruleup.verification.presentation.permission.viewmodel

import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.verification.domain.entity.PermissionSnapshot
import com.ruleup.verification.domain.entity.PermissionState
import com.ruleup.verification.domain.repository.PermissionStatusProvider
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

/**
 * 권한 재연결 화면의 전이. 이 화면은 사용자가 **설정에서 권한을 켜고 돌아오는** 동선이 본체라
 * 재조회가 곧 기능이다. 조회 실패를 어떻게 다루느냐가 이 화면의 핵심 계약이다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionRepairViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `아직 물어보기 전에는 권한을 안다고 하지 않는다`() {
        assertNull(viewModel().uiState.value.permissions)
    }

    @Test
    fun `재조회하면 지금 기기의 권한 현황으로 갈아끼운다`() =
        runTest {
            val viewModel = viewModel(provider = { snapshot(usageStats = PermissionState.DENIED) })

            viewModel.onIntent(PermissionRepairIntent.Refresh)

            assertEquals(
                PermissionState.DENIED,
                viewModel.uiState.value.permissions
                    ?.usageStats,
            )
        }

    @Test
    fun `조회에 실패하면 직전 현황을 그대로 둔다`() =
        runTest {
            // 모른다고 "다 끊겼다"로 그리면 사용자는 없던 사고를 복구하러 설정을 헤맨다.
            var fail = false
            val viewModel =
                viewModel(
                    provider = {
                        if (fail) throw IllegalStateException("OS 조회 실패") else snapshot()
                    },
                )
            viewModel.onIntent(PermissionRepairIntent.Refresh)

            fail = true
            viewModel.onIntent(PermissionRepairIntent.Refresh)

            assertEquals(
                PermissionState.GRANTED,
                viewModel.uiState.value.permissions
                    ?.location,
            )
        }

    @Test
    fun `뒤로 가기는 화면을 떠나는 것 말고 다른 일을 하지 않는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(nav = nav).onIntent(PermissionRepairIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun viewModel(
        provider: suspend () -> PermissionSnapshot = { snapshot() },
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = PermissionRepairViewModel(
        permissionStatusProvider = PermissionStatusProvider { provider() },
        navigationHelper = nav,
    )

    private fun snapshot(usageStats: PermissionState = PermissionState.GRANTED) =
        PermissionSnapshot(
            location = PermissionState.GRANTED,
            backgroundLocation = PermissionState.GRANTED,
            activityRecognition = PermissionState.GRANTED,
            usageStats = usageStats,
            postNotifications = PermissionState.GRANTED,
            healthDistance = PermissionState.GRANTED,
            healthSteps = PermissionState.GRANTED,
            healthSleep = PermissionState.GRANTED,
            healthBackground = PermissionState.GRANTED,
        )
}
